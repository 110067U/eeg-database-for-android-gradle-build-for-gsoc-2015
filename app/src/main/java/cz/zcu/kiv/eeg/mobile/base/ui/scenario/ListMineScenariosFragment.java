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
package cz.zcu.kiv.eeg.mobile.base.ui.scenario;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.SearchView;
import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.CommonActivity;
import cz.zcu.kiv.eeg.mobile.base.data.Values;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ScenarioAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Scenario;
import cz.zcu.kiv.eeg.mobile.base.utils.ConnectionUtils;
import cz.zcu.kiv.eeg.mobile.base.ws.asynctask.FetchScenarios;

import java.util.ArrayList;

/**
 * Fragment for listing all user's scenarios.
 * Data are displayed in list and can be filtered by query string.
 * Details are displayed in own activity on devices with small display, in details fragment otherwise.
 *
 * @author Petr Miko
 */
public class ListMineScenariosFragment extends ListFragment implements SearchView.OnQueryTextListener, View.OnClickListener {

    private final static String TAG = ListMineScenariosFragment.class.getSimpleName();
    private static ScenarioAdapter adapter;
    private boolean isDualView;
    private int cursorPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            cursorPosition = savedInstanceState.getInt("cursorMinePosition", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.base_scenario_list, container, false);
        View detailsFrame = view.findViewById(R.id.details);
        isDualView = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        View emptyView = view.findViewById(android.R.id.empty);
        emptyView.setOnClickListener(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setListAdapter(null);
        setListAdapter(getAdapter());

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        if (isDualView) {
            listView.setSelector(R.drawable.list_selector);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        listView.setTextFilterEnabled(true);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (isDualView) {
            showDetails(cursorPosition);
            setSelection(cursorPosition);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scenario_refresh:
                update();
                Log.d(TAG, "Refresh data button pressed");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case android.R.id.empty:
                update();
                break;
        }
    }

    /**
     * If online, fetches available user's scenarios.
     */
    private void update() {

        CommonActivity activity = (CommonActivity) getActivity();
        if (ConnectionUtils.isOnline(activity)) {
            new FetchScenarios(activity, getAdapter(), Values.SERVICE_QUALIFIER_MINE).execute();
        } else
            activity.showAlert(activity.getString(R.string.error_offline));
    }

    /**
     * Scenario adapter getter.
     * Instance is created in moment of first invocation.
     *
     * @return scenario adapter
     */
    private ScenarioAdapter getAdapter() {
        if (adapter == null) {
            adapter = new ScenarioAdapter(getActivity(), R.layout.base_scenario_row, new ArrayList<Scenario>());
        }
        return adapter;
    }

    /**
     * Method to show the details of a selected item.
     * Details are displayed either in-place in the current UI fragment, or new ScenarioDetailsActivity is created.
     *
     * @param index index of selected item in list
     */
    void showDetails(int index) {
        cursorPosition = index;

        ScenarioAdapter dataAdapter = getAdapter();
        boolean empty = dataAdapter == null || dataAdapter.isEmpty();

        if (isDualView) {
            getListView().setItemChecked(index, true);

            ScenarioDetailsFragment details = new ScenarioDetailsFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            if (getFragmentManager().findFragmentByTag(ScenarioDetailsFragment.TAG) == null) {
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            }

            Bundle args = new Bundle();
            args.putInt("index", index);
            args.putParcelable("data", empty ? null : dataAdapter.getItem(index));
            details.setArguments(args);

            ft.replace(R.id.details, details, ScenarioDetailsFragment.TAG);
            ft.commit();

        } else if (!empty) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), ScenarioDetailsActivity.class);
            intent.putExtra("index", index);
            intent.putExtra("data", dataAdapter.getItem(index));
            startActivity(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("cursorMinePosition", cursorPosition);
    }

    /**
     * On click displays selected item details.
     *
     * @param listView event list view source (omitted here)
     * @param view     event view source (omitted here)
     * @param position position of selected item in list view
     * @param id       list item identifier
     */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        showDetails(position);
        this.setSelection(position);
    }

    /**
     * Adds to options menu search possibility.
     *
     * @param menu     menu to extend
     * @param inflater menu inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scenario_mine_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem search = menu.findItem(R.id.scenario_search);
        SearchView searchView = new SearchView(getActivity());
        searchView.setOnQueryTextListener(this);
        search.setActionView(searchView);
    }

    /**
     * If query is not empty, on submit hides keyboard.
     *
     * @param query filter query
     * @return event handled
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        return true;
    }

    /**
     * Adds filtering after any changes to filter string.
     *
     * @param newText updated filter string
     * @return event handled
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        getAdapter().getFilter().filter(newText);
        return true;
    }
}
