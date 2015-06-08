/***********************************************************************************************************************
 *
 * This file is part of the eeg-database-for-android project

 * ==========================================
 *
 * Copyright (C) 2013 by University of West Bohemia (http://www.zcu.cz/en/)
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
 *
 * Petr Ježek, Petr Miko
 *
 **********************************************************************************************************************/
package cz.zcu.kiv.eeg.mobile.base.ws.asynctask;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.util.Log;
import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.CommonActivity;
import cz.zcu.kiv.eeg.mobile.base.archetypes.CommonService;
import cz.zcu.kiv.eeg.mobile.base.data.Values;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ReservationAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Reservation;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.ReservationList;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.TimeContainer;
import cz.zcu.kiv.eeg.mobile.base.ui.reservation.ReservationDetailsFragment;
import cz.zcu.kiv.eeg.mobile.base.ws.ssl.SSLSimpleClientHttpRequestFactory;
import org.springframework.http.*;
import org.springframework.http.converter.xml.SimpleXmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static cz.zcu.kiv.eeg.mobile.base.data.ServiceState.*;

/**
 * Service (AsyncTask) for fetching reservations created to specified day.
 *
 * @author Petr Miko
 */
public class FetchReservationsToDate extends CommonService<TimeContainer, Void, List<Reservation>> {

    private static final String TAG = FetchReservationsToDate.class.getSimpleName();
    private ReservationAdapter reservationAdapter;

    /**
     * Constructor.
     *
     * @param activity           parent activity
     * @param reservationAdapter adapter into which should be stored fetched reservations
     */
    public FetchReservationsToDate(CommonActivity activity, ReservationAdapter reservationAdapter) {
        super(activity);
        this.reservationAdapter = reservationAdapter;
    }

    /**
     * Method, where all reservations to specified date are read from server.
     * All heavy lifting is made here.
     *
     * @param params only one TimeContainer parameter is allowed here - specifies day, month and year
     * @return list of fetched reservations
     */
    @Override
    protected List<Reservation> doInBackground(TimeContainer... params) {
        SharedPreferences credentials = getCredentials();
        String username = credentials.getString("username", null);
        String password = credentials.getString("password", null);
        String url = credentials.getString("url", null) + Values.SERVICE_RESERVATION;

        if (params.length == 1) {
            TimeContainer time = params[0];
            url = url + time.getDay() + "-" + time.getMonth() + "-" + time.getYear();
        } else {
            Log.e(TAG, "Invalid params count! There must be one TimeContainer instance");
            setState(ERROR, "Invalid params count! There must be one TimeContainer instance");
            return Collections.emptyList();
        }

        setState(RUNNING, R.string.working_ws_msg);

        // Populate the HTTP Basic Authentication header with the username and
        // password
        HttpAuthentication authHeader = new HttpBasicAuthentication(username, password);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setAuthorization(authHeader);
        requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));

        SSLSimpleClientHttpRequestFactory factory = new SSLSimpleClientHttpRequestFactory();
        // Create a new RestTemplate instance
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(new SimpleXmlHttpMessageConverter());

        try {
            // Make the network request
            Log.d(TAG, url);
            ResponseEntity<ReservationList> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<Object>(requestHeaders), ReservationList.class);
            ReservationList body = response.getBody();

            if (body != null) {
                return body.getReservations();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            setState(ERROR, e);
        } finally {
            setState(DONE);
        }
        return Collections.emptyList();
    }

    /**
     * Clears adapter of current data and fills it with fetched reservations.
     * In process it clears details fragment, so it could not display information about no longer existing reservation.
     *
     * @param resultList fetched reservations
     */
    @Override
    protected void onPostExecute(List<Reservation> resultList) {
        reservationAdapter.clear();
        if (resultList != null && !resultList.isEmpty()) {
            for (Reservation reservation : resultList) {
                try {
                    reservationAdapter.add(reservation);
                } catch (Exception e) {
                    setState(ERROR, e);
                    Log.e(TAG, e.getLocalizedMessage(), e);
                }
            }
        }

        FragmentManager fm = activity.getFragmentManager();

        ReservationDetailsFragment details = new ReservationDetailsFragment();
        ReservationDetailsFragment frag = (ReservationDetailsFragment) fm.findFragmentByTag(ReservationDetailsFragment.TAG);
        if (frag != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.details, details, ReservationDetailsFragment.TAG);
            ft.commit();
        }
    }
}
