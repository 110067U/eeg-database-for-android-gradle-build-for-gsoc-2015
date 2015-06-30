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
package cz.zcu.kiv.eeg.mobile.base.ui.experiment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.CommonActivity;
import cz.zcu.kiv.eeg.mobile.base.archetypes.SaveDiscardActivity;
import cz.zcu.kiv.eeg.mobile.base.data.Values;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.*;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.*;
import cz.zcu.kiv.eeg.mobile.base.localdb.CBDatabase;
import cz.zcu.kiv.eeg.mobile.base.ui.person.PersonAddActivity;
import cz.zcu.kiv.eeg.mobile.base.ui.scenario.ScenarioAddActivity;
import cz.zcu.kiv.eeg.mobile.base.utils.ConnectionUtils;
import cz.zcu.kiv.eeg.mobile.base.utils.Keys;
import cz.zcu.kiv.eeg.mobile.base.utils.ValidationUtils;
import cz.zcu.kiv.eeg.mobile.base.ws.asynctask.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for creating new experiment on eeg base.
 *
 * @author Petr Miko
 */
public class ExperimentAddActivity extends SaveDiscardActivity implements View.OnClickListener {

    //all the necessary fields
    private static ResearchGroupAdapter groupAdapter;
    private static ScenarioAdapter scenarioAdapter;
    private static PersonAdapter personAdapter;
    private static ArtifactAdapter artifactAdapter;
    private static DigitizationAdapter digitizationAdapter;
    private static HardwareAdapter hardwareAdapter;
    private static WeatherAdapter weatherAdapter;
    private static List<Hardware> selectedHardware = new ArrayList<Hardware>();
    private static SoftwareAdapter softwareAdapter;
    private static List<Software> selectedSoftware = new ArrayList<Software>();
    private static DiseaseAdapter diseaseAdapter;
    private static List<Disease> selectedDiseases = new ArrayList<Disease>();
    private static PharmaceuticalAdapter pharmaceuticalAdapter;
    private static List<Pharmaceutical> selectedPharmaceuticals = new ArrayList<Pharmaceutical>();
    private static ElectrodeLocationAdapter electrodeLocationAdapter;
    private static List<ElectrodeLocation> selectedElectrodeLocations = new ArrayList<ElectrodeLocation>();
    private static ElectrodeSystemAdapter electrodeSystemAdapter;
    private TimeContainer fromTime;
    private TimeContainer toTime;
    private CBDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_experiment_add);

        initView(savedInstanceState);
        updateData();

    }

    /**
     *
     * If not currently working and data container are not empty, fetches data from server.
     */
    private void updateData() {
        //services are not loading data currently
        if (!isWorking()) {
            if (groupAdapter.isEmpty())
                updateGroups();
            if (scenarioAdapter.isEmpty())
                updateScenarios();
            if (personAdapter.isEmpty())
                updateSubjects();
            if (artifactAdapter.isEmpty())
                updateArtifacts();
            if (digitizationAdapter.isEmpty())
                updateDigitizations();
            if (electrodeSystemAdapter.isEmpty())
                updateElectrodeSystems();
        }
    }

    /**
     * Inits view with proper data, sets onClick listeners and assigns array adapters.
     *
     * @param savedInstanceState previous instance state
     */
    private void initView(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            fromTime = new TimeContainer();


            Time tmpTime = fromTime.getTime();
            tmpTime.minute += 30;
            //removing correction of fromTime
            tmpTime.month -= 1;
            tmpTime.normalize(false);

            toTime = new TimeContainer(tmpTime);

        } else {
            fromTime = savedInstanceState.getParcelable("fromTime");
            toTime = savedInstanceState.getParcelable("toTime");
        }

        Button fromTimeButton = (Button) findViewById(R.id.experiment_add_from_time);
        Button fromDateButton = (Button) findViewById(R.id.experiment_add_from_date);
        Button toTimeButton = (Button) findViewById(R.id.experiment_add_to_time);
        Button toDateButton = (Button) findViewById(R.id.experiment_add_to_date);
        fromTimeButton.setOnClickListener(this);
        fromDateButton.setOnClickListener(this);
        toTimeButton.setOnClickListener(this);
        toDateButton.setOnClickListener(this);

        //set time and date to buttons so user sees defaults on start
        fromTimeButton.setText(fromTime.toTimeString());
        fromDateButton.setText(fromTime.toDateString());
        toTimeButton.setText(toTime.toTimeString());
        toDateButton.setText(toTime.toDateString());


        ImageButton createScenario = (ImageButton) findViewById(R.id.experiment_add_scenario_new);
        ImageButton createSubject = (ImageButton) findViewById(R.id.experiment_add_subject_new);
        ImageButton createElectrodeLocation = (ImageButton) findViewById(R.id.experiment_add_electrode_new_location_button);
        ImageButton createArtifact = (ImageButton) findViewById(R.id.experiment_add_artifact_new);
        ImageButton createDisesase = (ImageButton) findViewById(R.id.experiment_add_disease_new_button);
        ImageButton createDigitization = (ImageButton) findViewById(R.id.experiment_add_digitization_new);
        ImageButton createWeather = (ImageButton) findViewById(R.id.experiment_add_weather_new);

        createScenario.setOnClickListener(this);
        createSubject.setOnClickListener(this);
        createElectrodeLocation.setOnClickListener(this);
        createArtifact.setOnClickListener(this);
        createDisesase.setOnClickListener(this);
        createDigitization.setOnClickListener(this);
        createWeather.setOnClickListener(this);

        Spinner scenarios = (Spinner) findViewById(R.id.experiment_add_scenario);
        Spinner groups = (Spinner) findViewById(R.id.experiment_add_group);
        Spinner subjects = (Spinner) findViewById(R.id.experiment_add_subject);
        Spinner artifacts = (Spinner) findViewById(R.id.experiment_add_artifact);
        Spinner digitizations = (Spinner) findViewById(R.id.experiment_add_digitization);
        Spinner electrodeSystems = (Spinner) findViewById(R.id.experiment_add_electrode_system);
        Spinner weathers = (Spinner) findViewById(R.id.experiment_add_weather);

        groups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //refresh weather records
                updateWeatherList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing
            }
        });


        scenarios.setAdapter(getScenarioAdapter());
        groups.setAdapter(getGroupAdapter());
        subjects.setAdapter(getPersonAdapter());
        artifacts.setAdapter(getArtifactAdapter());
        digitizations.setAdapter(getDigitizationAdapter());
        electrodeSystems.setAdapter(getElectrodeSystemAdapter());
        weathers.setAdapter(getWeatherAdapter());

        Button selectHw = (Button) findViewById(R.id.experiment_add_hardware_button);
        selectHw.setOnClickListener(this);
        Button selectSw = (Button) findViewById(R.id.experiment_add_software_button);
        selectSw.setOnClickListener(this);
        Button selectDiseases = (Button) findViewById(R.id.experiment_add_disease_button);
        selectDiseases.setOnClickListener(this);
        Button selectPharmaceuticals = (Button) findViewById(R.id.experiment_add_pharmaceutical_button);
        selectPharmaceuticals.setOnClickListener(this);
        Button selectElectrodeLocations = (Button) findViewById(R.id.experiment_add_electrode_location_button);
        selectElectrodeLocations.setOnClickListener(this);

        //fill multiselections from previously choosen data
        fillHardwareListRows();
        fillSoftwareListRows();
        fillDiseasesRows();
        fillPharmaceuticalsRows();
        fillElectrodeLocationsRows();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        Spinner groupSpinner = (Spinner) findViewById(R.id.experiment_add_group);
        ResearchGroup group = (ResearchGroup) groupSpinner.getSelectedItem();
        switch (v.getId()) {
            case R.id.experiment_add_from_date:
                showDateDialog((Button) findViewById(R.id.experiment_add_from_date), fromTime);
                break;

            case R.id.experiment_add_from_time:
                showTimeDialog((Button) findViewById(R.id.experiment_add_from_time), fromTime);
                break;

            case R.id.experiment_add_to_date:
                showDateDialog((Button) findViewById(R.id.experiment_add_to_date), toTime);
                break;

            case R.id.experiment_add_to_time:
                showTimeDialog((Button) findViewById(R.id.experiment_add_to_time), toTime);
                break;

            case R.id.experiment_add_scenario_new:                                          //Done
                intent = new Intent();
                intent.setClass(this, ScenarioAddActivity.class);
                startActivityForResult(intent, Values.ADD_SCENARIO_FLAG);
                break;

            case R.id.experiment_add_subject_new:                                           //Done
                if (group != null) {
                    intent = new Intent();
                    intent.setClass(this, PersonAddActivity.class);
                    intent.putExtra("groupId", group.getGroupId());
                    startActivityForResult(intent, Values.ADD_PERSON_FLAG);
                } else showAlert(getString(R.string.error_no_group_selected));
                break;

            case R.id.experiment_add_hardware_button:
                selectHardwareDialog();
                break;

            case R.id.experiment_add_software_button:
                selectSoftwareDialog();
                break;

            case R.id.experiment_add_disease_button:
                selectDiseaseDialog();
                break;

            case R.id.experiment_add_pharmaceutical_button:
                selectPharmaceuticalDialog();
                break;

            case R.id.experiment_add_electrode_location_button:
                selectElectrodeLocationsDialog();
                break;

            case R.id.experiment_add_electrode_new_location_button:                         //Done
                if (group != null) {
                    intent = new Intent();
                    intent.setClass(this, ElectrodeLocationAddActivity.class);
                    intent.putExtra("groupId", group.getGroupId());
                    startActivityForResult(intent, Values.ADD_ELECTRODE_LOCATION_FLAG);
                }else showAlert(getString(R.string.error_no_group_selected));
                break;

                case R.id.experiment_add_digitization_new:                                  //Done
                if (group != null) {
                    intent = new Intent();
                    intent.setClass(this, DigitizationAddActivity.class);
                    intent.putExtra("groupId", group.getGroupId());
                    startActivityForResult(intent, Values.ADD_DIGITIZATION_FLAG);
                }else showAlert(getString(R.string.error_no_group_selected));
                break;

            case R.id.experiment_add_disease_new_button:                                    //Done
                if (group != null) {
                    intent = new Intent();
                    intent.setClass(this, DiseaseAddActivity.class);
                    intent.putExtra("groupId", group.getGroupId());
                    startActivityForResult(intent, Values.ADD_DISEASE_FLAG);
                }else showAlert(getString(R.string.error_no_group_selected));
                break;

            case R.id.experiment_add_artifact_new:                                          //Done
                if (group != null) {
                    intent = new Intent();
                    intent.setClass(this, ArtifactAddActivity.class);
                    intent.putExtra("groupId", group.getGroupId());
                    startActivityForResult(intent, Values.ADD_ARTIFACT_FLAG);
                } else showAlert(getString(R.string.error_no_group_selected));
                break;

            case R.id.experiment_add_weather_new:                                           //Done
                if (group != null) {
                    intent = new Intent();
                    intent.setClass(this, WeatherAddActivity.class);
                    intent.putExtra("groupId", group.getGroupId());
                    startActivityForResult(intent, Values.ADD_WEATHER_FLAG);
                } else showAlert(getString(R.string.error_no_group_selected));
                break;
        }
        super.onClick(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.exp_add_menu, menu);
        return true;
    }

    /**
     * Adds refresh selection handling.
     * If refresh is selected, all array adapters are updated with data from server.
     *
     * @param item selected item
     * @return event handled or not
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                if (!isWorking()) {
                    updateGroups();
                    updateScenarios();
                    updateSubjects();
                    updateArtifacts();
                    updateDigitizations();
                    updateElectrodeSystems();
                    updateElectrodeLocations();
                    updateDiseases();
                    updateHardwareList();
                    updateSoftwareList();
                    updatePharmaceuticals();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reads values from fields and if valid, proceeds with creation of experiment on server.
     */
    @Override
    protected void save() {

        SharedPreferences getOwner = getSharedPreferences(Values.PREFS_TEMP, Context.MODE_PRIVATE);
        String loggeduserDocID    = getOwner.getString("loggedUserDocID", null);

        Experiment experiment;
        if ((experiment = getValidRecord()) != null) {
//            if (ConnectionUtils.isOnline(this)) {
//                new CreateExperiment(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, experiment);

//===================================================================Database Code==================================================//
                db = new CBDatabase(Keys.DB_NAME, ExperimentAddActivity.this);
                // create an object that contains data for a document
                Map<String, Object> docContent = new HashMap<String, Object>();
                docContent.put("type", "Experiment");
                docContent.put("research_group_id", experiment.getResearchGroup().getGroupId());        //Done
                docContent.put("scenario_id", experiment.getScenario().getScenarioId());                //Done
                docContent.put("owner_id", loggeduserDocID);                                            //Done
                docContent.put("start_time", experiment.getStartTime().toString());                     //Done
                docContent.put("end_time", experiment.getEndTime().toString());                         //Done
                docContent.put("subject_id", experiment.getSubject().getPersonId());                    //Done
                docContent.put("weather_id", experiment.getWeather().getWeatherId());                   //Done
                docContent.put("temperature", experiment.getTemperature());                             //Done
                docContent.put("artifact_id", experiment.getArtifact().getArtifactId());                //Done
                docContent.put("digitization_id", experiment.getDigitization().getDigitizationId());    //Done
                docContent.put("environment_note", experiment.getEnvironmentNote());                    //Done

                List<String> hardware_ids = new ArrayList<String>();
                if (experiment.getHardwareList().getHardwareList() != null && !experiment.getHardwareList().getHardwareList().isEmpty()){
                    for (int i = 0; i <experiment.getHardwareList().getHardwareList().size() ; i++) {
                        hardware_ids.add(experiment.getHardwareList().getHardwareList().get(i).getHardwareId());
                    }
                }
                docContent.put("hardware_list", hardware_ids);                                          //Done

                List<String> software_ids = new ArrayList<String>();
                if (experiment.getSoftwareList().getSoftwareList() != null && !experiment.getSoftwareList().getSoftwareList().isEmpty()){
                    for (int i = 0; i <experiment.getSoftwareList().getSoftwareList().size() ; i++) {
                        software_ids.add(experiment.getSoftwareList().getSoftwareList().get(i).getId());
                    }
                }
                docContent.put("software_list", software_ids);                                          //Done

                List<String> disease_ids = new ArrayList<String>();
                if (experiment.getDiseases().getDiseases() != null && !experiment.getDiseases().getDiseases().isEmpty()){
                    for (int i = 0; i <experiment.getDiseases().getDiseases().size(); i++) {
                        disease_ids.add(experiment.getDiseases().getDiseases().get(i).getDiseaseId());
                    }
                }
                docContent.put("disease_list", disease_ids);                                            //Done

                List<String> pharmaceutical_ids = new ArrayList<String>();
                if (experiment.getPharmaceuticals().getPharmaceuticals() != null && !experiment.getPharmaceuticals().getPharmaceuticals().isEmpty()){
                    for (int i = 0; i <experiment.getPharmaceuticals().getPharmaceuticals().size(); i++) {
                        pharmaceutical_ids.add(experiment.getPharmaceuticals().getPharmaceuticals().get(i).getId());
                    }
                }
                docContent.put("pharmaceutical_list", pharmaceutical_ids);                              //Done

                //Creating the Electrode_conf document and get its id
                Map<String, Object> electrodeConfDocContent = new HashMap<String, Object>();
                electrodeConfDocContent.put("type","ElectrodeConf");
                electrodeConfDocContent.put("impedance", experiment.getElectrodeConf().getImpedance());
                electrodeConfDocContent.put("electrode_system_id", experiment.getElectrodeConf().getElectrodeSystem().getId());

                List<String> electrode_location_ids = new ArrayList<String>();
                if (experiment.getElectrodeConf().getElectrodeLocations().getElectrodeLocations() != null && !experiment.getElectrodeConf().getElectrodeLocations().getElectrodeLocations().isEmpty()){
                    for (int i = 0; i <experiment.getElectrodeConf().getElectrodeLocations().getElectrodeLocations().size(); i++) {
                        electrode_location_ids.add(experiment.getElectrodeConf().getElectrodeLocations().getElectrodeLocations().get(i).getId());
                    }
                }
                electrodeConfDocContent.put("electrode_locations_list", electrode_location_ids);

                String electrode_conf_id = null;
                try{
                    electrode_conf_id = db.create(electrodeConfDocContent);
                    assert(electrode_conf_id != null);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                docContent.put("electrode_conf_id", electrode_conf_id);


                String experimentID = null;
                try {
                    // Create a new document
                    experimentID = db.create(docContent);
//                    Log.i("eeg_experiment_id",experimentID+"");
//                    Map<String, Object> testMe = db.retrieve(experimentID);
//                    Log.i("eeg_exp_doc_content",testMe.toString()+"");

                    assert(experimentID != null);
                    Intent resultIntent = new Intent();
                    experiment.setExperimentId(experimentID);
                    resultIntent.putExtra(Values.ADD_EXPERIMENT_KEY, experiment);
                    Toast.makeText(ExperimentAddActivity.this, R.string.creation_experiment_ok, Toast.LENGTH_SHORT).show();
                    ExperimentAddActivity.this.setResult(Activity.RESULT_OK, resultIntent);
                    ExperimentAddActivity.this.finish();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ExperimentAddActivity.this, R.string.creation_failed, Toast.LENGTH_SHORT).show();
                }
// ================================================================================================================================//

                selectedElectrodeLocations = new ArrayList<ElectrodeLocation>();
                selectedHardware = new ArrayList<Hardware>();
                selectedSoftware = new ArrayList<Software>();
                selectedPharmaceuticals = new ArrayList<Pharmaceutical>();
                selectedDiseases = new ArrayList<Disease>();
//            } else
//                showAlert(getString(R.string.error_offline));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void discard() {
        finish();
    }

    /**
     * Handles results from other activities.
     * Used when creating new records in other activities - here are new records extracted and put into proper array adapter.
     *
     * @param requestCode flag from closed activity
     * @param resultCode  code of activity result type (eg. Activity.RESULT_OK)
     * @param data        source intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Values.ADD_SCENARIO_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    Scenario record = (Scenario) data.getExtras().get(Values.ADD_SCENARIO_KEY);
                    getScenarioAdapter().add(record);
                }
                break;
            case Values.ADD_PERSON_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    Person record = (Person) data.getExtras().get(Values.ADD_PERSON_KEY);
                    getPersonAdapter().add(record);
                }
                break;

            case Values.ADD_ARTIFACT_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    Artifact record = (Artifact) data.getExtras().get(Values.ADD_ARTIFACT_KEY);
                    getArtifactAdapter().add(record);
                }
                break;

            case Values.ADD_DIGITIZATION_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    Digitization record = (Digitization) data.getExtras().get(Values.ADD_DIGITIZATION_KEY);
                    getDigitizationAdapter().add(record);
                }
                break;

            case Values.ADD_DISEASE_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    Disease record = (Disease) data.getExtras().get(Values.ADD_DISEASE_KEY);
                    getDiseaseAdapter().add(record);
                }
                break;

            case Values.ADD_ELECTRODE_LOCATION_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    ElectrodeLocation record = (ElectrodeLocation) data.getExtras().get(Values.ADD_ELECTRODE_LOCATION_KEY);
                    getElectrodeLocationsAdapter().add(record);
                }
                break;
            case Values.ADD_WEATHER_FLAG:
                if (resultCode == Activity.RESULT_OK) {
                    Weather record = (Weather) data.getExtras().get(Values.ADD_WEATHER_KEY);
                    getWeatherAdapter().add(record);
                }
                break;
        }
    }

    /**
     * Stores times on instance destroy.
     *
     * @param outState instance state to be saved
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("fromTime", fromTime);
        outState.putParcelable("toTime", toTime);
    }


    /* --------------------------------- Time choice dialogs ------------------ */

    /**
     * Displays time picking dialog.
     *
     * @param timeButton    event source time button
     * @param timeContainer time container to save information into
     */
    private void showTimeDialog(final Button timeButton, final TimeContainer timeContainer) {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                timeContainer.setHour(hourOfDay);
                timeContainer.setMinute(minute);
                timeButton.setText(timeContainer.toTimeString());
            }
        }, timeContainer.getHour(), timeContainer.getMinute(), true).show();
    }

    /**
     * Displays date picking dialog.
     *
     * @param dateButton    event source date button
     * @param timeContainer time container to save information into
     */
    private void showDateDialog(final Button dateButton, final TimeContainer timeContainer) {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                timeContainer.setYear(year);
                timeContainer.setMonth(monthOfYear + 1);
                timeContainer.setDay(dayOfMonth);

                dateButton.setText(timeContainer.toDateString());

            }
        }, timeContainer.getYear(), timeContainer.getMonth() - 1, timeContainer.getDay()).show();
    }

    /* --------------------------------- CommonService invoke-methods ------------------ */

    /**
     * Updates scenario array adapter. fetch all scenarios (all ? what is the criteria)
     */
    private void updateScenarios() {
        //Fetch from local db
        db = new CBDatabase(Keys.DB_NAME, ExperimentAddActivity.this);
        db.createScenarioView("fetchAllScenariosView", "Scenario",getScenarioAdapter());
//        if (ConnectionUtils.isOnline(this)) {
//            new FetchScenarios(this, getScenarioAdapter(), Values.SERVICE_QUALIFIER_ALL).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        } else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates research group array adapter.
     */
    private void updateGroups() {
        //Fetch from local db
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createResearchGroupView("fetchResearchGruopsView", "Research Group", getGroupAdapter());
//        if (ConnectionUtils.isOnline(this)) {
//            new FetchResearchGroups(this, getGroupAdapter(), Values.SERVICE_QUALIFIER_MINE).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        } else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates subjects' array adapter.
     */
    private void updateSubjects() {
        //Fetch from local db
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createPersonView("fetchPersonView", "Person", getPersonAdapter());
//        if (ConnectionUtils.isOnline(this)) {
//            new FetchPeople(this, getPersonAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        } else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates artifact array adapter.
     */
    private void updateArtifacts() {
        //Fetch from local db
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createArtifactView("fetchAllArtifactView", "Artifact", getArtifactAdapter());

//        if (ConnectionUtils.isOnline(this)) {
//            new FetchArtifacts(this, getArtifactAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        } else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates digitization array adapter.
     */
    private void updateDigitizations() {
        //Fetch from local db
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createDigitizationView("fetchAllDigitizationView", "Digitization", getDigitizationAdapter());

//        if (ConnectionUtils.isOnline(this)) {
//            new FetchDigitizations(this, getDigitizationAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        } else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates hardware array adapter.
     */
    private void updateHardwareList() {
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createHardwareView("fetchAllHardwareView", "Hardware", getHardwareAdapter());
//        if (ConnectionUtils.isOnline(this))
//            new FetchHardwareList(this, getHardwareAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates software array adapter.
     */
    private void updateSoftwareList() {
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createSoftwareView("fetchAllSoftwareView", "Software", getSoftwareAdapter());
//        if (ConnectionUtils.isOnline(this))
//            new FetchSoftwareList(this, getSoftwareAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates disease array adapter.
     */
    private void updateDiseases() {
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createDiseasesView("fetchAllDiseasesView", "Disease", getDiseaseAdapter());
//        if (ConnectionUtils.isOnline(this))
//            new FetchDiseases(this, getDiseaseAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates pharmaceuticals' array adapter.
     */
    private void updatePharmaceuticals() {
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createPharmaceuticalsView("fetchAllPharmaceuticalsView", "Pharmaceutical", getPharmaceuticalAdapter());
        //        if (ConnectionUtils.isOnline(this))
//            new FetchPharmaceuticals(this, getPharmaceuticalAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates electrodes array adapter.
     */
    private void updateElectrodeSystems() {
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createElectrodeSystemView("fetchAllElectrodeSystemRecordsView", "ElectrodeSystem", getElectrodeSystemAdapter());
//        if (ConnectionUtils.isOnline(this))
//            new FetchElectrodeSystems(this, getElectrodeSystemAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        else
//            showAlert(getString(R.string.error_offline));
    }

    /**
     * Updates electrode locations array adapter.
     */
    private void updateElectrodeLocations() {
        CommonActivity activity = (CommonActivity) this;
        db = new CBDatabase(Keys.DB_NAME, activity);
        db.createElectrodeLocationView("fetchAllElectrodeLocationRecordsView", "ElectrodeLocation", getElectrodeLocationsAdapter());
//        if (ConnectionUtils.isOnline(this))
//            new FetchElectrodeLocations(this, getElectrodeLocationsAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        else
//            showAlert(getString(R.string.error_offline));
    }

    private void updateWeatherList() {
        Spinner groups = (Spinner) findViewById(R.id.experiment_add_group);
        ResearchGroup group = (ResearchGroup) groups.getSelectedItem();
        if (group == null){
            showAlert(getString(R.string.error_no_group_selected));
        }else{
            //Fetch from local db
            CommonActivity activity = (CommonActivity) this;
            db = new CBDatabase(Keys.DB_NAME, activity);
            db.createWeatherView("fetchWeatherRecordsView", "Weather", getWeatherAdapter(), group.getGroupId());
        }
//            new FetchWeatherList(this, group.getGroupId(), getWeatherAdapter()).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /* --------------------------------- Data adapters ------------------ */

    /**
     * Getter of scenario adapter. If not created yet, new instance is made.
     *
     * @return scenario array adapter
     */
    private ScenarioAdapter getScenarioAdapter() {
        if (scenarioAdapter == null) {
            scenarioAdapter = new ScenarioAdapter(this, R.layout.base_scenario_row, new ArrayList<Scenario>());
        }
        return scenarioAdapter;
    }

    /**
     * Getter of research group adapter. If not created yet, new instance is made.
     *
     * @return research group array adapter
     */
    private ResearchGroupAdapter getGroupAdapter() {
        if (groupAdapter == null) {
            groupAdapter = new ResearchGroupAdapter(this, R.layout.base_row_simple, new ArrayList<ResearchGroup>());
        }

        return groupAdapter;
    }

    /**
     * Getter of person adapter. If not created yet, new instance is made.
     *
     * @return person array adapter
     */
    private PersonAdapter getPersonAdapter() {
        if (personAdapter == null)
            personAdapter = new PersonAdapter(this, R.layout.base_person_row, new ArrayList<Person>());

        return personAdapter;
    }

    /**
     * Getter of artifact adapter. If not created yet, new instance is made.
     *
     * @return artifact array adapter
     */
    private ArtifactAdapter getArtifactAdapter() {
        if (artifactAdapter == null)
            artifactAdapter = new ArtifactAdapter(this, R.layout.base_artifact_row, new ArrayList<Artifact>());

        return artifactAdapter;
    }

    /**
     * Getter of digitization adapter. If not created yet, new instance is made.
     *
     * @return digitization array adapter
     */
    private DigitizationAdapter getDigitizationAdapter() {
        if (digitizationAdapter == null)
            digitizationAdapter = new DigitizationAdapter(this, R.layout.base_digitization_row, new ArrayList<Digitization>());

        return digitizationAdapter;
    }

    /**
     * Getter of hardware adapter. If not created yet, new instance is made.
     *
     * @return hardware array adapter
     */
    private HardwareAdapter getHardwareAdapter() {
        if (hardwareAdapter == null)
            hardwareAdapter = new HardwareAdapter(this, R.layout.base_hardware_row, new ArrayList<Hardware>());

        return hardwareAdapter;
    }

    /**
     * Getter of software adapter. If not created yet, new instance is made.
     *
     * @return software array adapter
     */
    private SoftwareAdapter getSoftwareAdapter() {
        if (softwareAdapter == null)
            softwareAdapter = new SoftwareAdapter(this, R.layout.base_software_row, new ArrayList<Software>());
        return softwareAdapter;
    }

    /**
     * Getter of disease adapter. If not created yet, new instance is made.
     *
     * @return disease array adapter
     */
    private DiseaseAdapter getDiseaseAdapter() {
        if (diseaseAdapter == null)
            diseaseAdapter = new DiseaseAdapter(this, R.layout.base_disease_row, new ArrayList<Disease>());
        return diseaseAdapter;
    }

    /**
     * Getter of pharmaceutical adapter. If not created yet, new instance is made.
     *
     * @return scenario array adapter
     */
    private PharmaceuticalAdapter getPharmaceuticalAdapter() {
        if (pharmaceuticalAdapter == null)
            pharmaceuticalAdapter = new PharmaceuticalAdapter(this, R.layout.base_pharmaceutical_row, new ArrayList<Pharmaceutical>());
        return pharmaceuticalAdapter;
    }

    /**
     * Getter of electrode system adapter. If not created yet, new instance is made.
     *
     * @return electrode system array adapter
     */
    private ElectrodeSystemAdapter getElectrodeSystemAdapter() {
        if (electrodeSystemAdapter == null)
            electrodeSystemAdapter = new ElectrodeSystemAdapter(this, R.layout.base_electrode_simple_row, new ArrayList<ElectrodeSystem>());
        return electrodeSystemAdapter;
    }

    /**
     * Getter of electrode location adapter. If not created yet, new instance is made.
     *
     * @return electrode location array adapter
     */
    private ElectrodeLocationAdapter getElectrodeLocationsAdapter() {
        if (electrodeLocationAdapter == null)
            electrodeLocationAdapter = new ElectrodeLocationAdapter(this, R.layout.base_electrode_location_row, new ArrayList<ElectrodeLocation>());
        return electrodeLocationAdapter;
    }

    /**
     * Getter of weather adapter. If not created yet, new instance is made.
     *
     * @return weather array adapter
     */
    private WeatherAdapter getWeatherAdapter() {
        if (weatherAdapter == null)
            weatherAdapter = new WeatherAdapter(this, R.layout.base_weather_row, new ArrayList<Weather>());

        return weatherAdapter;
    }

    /* --------------------------------- Multi-select spinners and handling their behaviour ------------------ */

    /**
     * Creates and displays dialog which supports multi selection.
     * The selection is visible in its linear layout afterwards.
     * <p/>
     * Positive button adds selection into linear layout.
     * Neutral button resets selection.
     * Negative button cancels dialog without change.
     * <p/>
     * Implementation for hardware.
     */
    private void selectHardwareDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.base_list, null, false);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setAdapter(getHardwareAdapter());

        if (!isWorking() && hardwareAdapter.isEmpty())
            updateHardwareList();

        dialog.setTitle(R.string.experiment_add_hardware);
        dialog.setView(dialogView);
        dialog.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                int len = listView.getCount();
                SparseBooleanArray checked = listView.getCheckedItemPositions();

                selectedHardware.clear();

                //find out selected items
                for (int i = 0; i < len; i++) {
                    if (checked.get(i))
                        selectedHardware.add(getHardwareAdapter().getItem(i));
                }

                fillHardwareListRows();
            }
        });

        dialog.setNeutralButton(R.string.dialog_button_clear_selection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                selectedHardware.clear();
                fillHardwareListRows();
                Toast.makeText(ExperimentAddActivity.this, R.string.dialog_selection_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        //reselect previously selected items
        for (Hardware hw : selectedHardware)
            for (int i = 0; i < hardwareAdapter.getCount(); i++) {
                if (hardwareAdapter.getItem(i).getHardwareId() == hw.getHardwareId()) {
                    listView.setItemChecked(i, true);
                }
            }
        dialog.show();
    }

    /**
     * Creates and displays dialog which supports multi selection.
     * The selection is visible in its linear layout afterwards.
     * <p/>
     * Positive button adds selection into linear layout.
     * Neutral button resets selection.
     * Negative button cancels dialog without change.
     * <p/>
     * Implementation for software.
     */
    private void selectSoftwareDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.base_list, null, false);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.setAdapter(getSoftwareAdapter());

        if (!isWorking() && softwareAdapter.isEmpty())
            updateSoftwareList();

        dialog.setTitle(R.string.experiment_add_software);
        dialog.setView(dialogView);
        dialog.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                int len = listView.getCount();
                SparseBooleanArray checked = listView.getCheckedItemPositions();

                selectedSoftware.clear();

                //find out selected items
                for (int i = 0; i < len; i++) {
                    if (checked.get(i))
                        selectedSoftware.add(getSoftwareAdapter().getItem(i));
                }

                fillSoftwareListRows();
            }
        });

        dialog.setNeutralButton(R.string.dialog_button_clear_selection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                selectedSoftware.clear();
                fillSoftwareListRows();
                Toast.makeText(ExperimentAddActivity.this, R.string.dialog_selection_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        //reselect previously selected items
        for (Software sw : selectedSoftware)
            for (int i = 0; i < softwareAdapter.getCount(); i++) {
                if (softwareAdapter.getItem(i).getId() == sw.getId()) {
                    listView.setItemChecked(i, true);
                }
            }
        dialog.show();
    }

    /**
     * Creates and displays dialog which supports multi selection.
     * The selection is visible in its linear layout afterwards.
     * <p/>
     * Positive button adds selection into linear layout.
     * Neutral button resets selection.
     * Negative button cancels dialog without change.
     * <p/>
     * Implementation for disease.
     */
    private void selectDiseaseDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.base_list, null, false);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setAdapter(getDiseaseAdapter());

        if (!isWorking() && diseaseAdapter.isEmpty())
            updateDiseases();

        dialog.setTitle(R.string.experiment_add_disease);
        dialog.setView(dialogView);
        dialog.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                int len = listView.getCount();
                SparseBooleanArray checked = listView.getCheckedItemPositions();

                selectedDiseases.clear();

                //find out selected items
                for (int i = 0; i < len; i++) {
                    if (checked.get(i))
                        selectedDiseases.add(getDiseaseAdapter().getItem(i));
                }

                fillDiseasesRows();
            }
        });

        dialog.setNeutralButton(R.string.dialog_button_clear_selection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                selectedDiseases.clear();
                fillDiseasesRows();
                Toast.makeText(ExperimentAddActivity.this, R.string.dialog_selection_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        //reselect previously selected items
        for (Disease disease : selectedDiseases)
            for (int i = 0; i < diseaseAdapter.getCount(); i++) {
                if (diseaseAdapter.getItem(i).getDiseaseId() == disease.getDiseaseId()) {
                    listView.setItemChecked(i, true);
                }
            }
        dialog.show();
    }

    /**
     * Creates and displays dialog which supports multi selection.
     * The selection is visible in its linear layout afterwards.
     * <p/>
     * Positive button adds selection into linear layout.
     * Neutral button resets selection.
     * Negative button cancels dialog without change.
     * <p/>
     * Implementation for pharmaceuticals.
     */
    private void selectPharmaceuticalDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.base_list, null, false);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setAdapter(getPharmaceuticalAdapter());

        if (!isWorking() && pharmaceuticalAdapter.isEmpty())
            updatePharmaceuticals();

        dialog.setTitle(R.string.experiment_add_pharmaceuticals);
        dialog.setView(dialogView);
        dialog.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                int len = listView.getCount();
                SparseBooleanArray checked = listView.getCheckedItemPositions();

                selectedPharmaceuticals.clear();

                //find out selected items
                for (int i = 0; i < len; i++) {
                    if (checked.get(i))
                        selectedPharmaceuticals.add(getPharmaceuticalAdapter().getItem(i));
                }

                fillPharmaceuticalsRows();
            }
        });

        dialog.setNeutralButton(R.string.dialog_button_clear_selection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                selectedPharmaceuticals.clear();
                fillPharmaceuticalsRows();
                Toast.makeText(ExperimentAddActivity.this, R.string.dialog_selection_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        //reselect previously selected items
        for (Pharmaceutical pharmaceutical : selectedPharmaceuticals)
            for (int i = 0; i < pharmaceuticalAdapter.getCount(); i++) {
                if (pharmaceuticalAdapter.getItem(i).getId() == pharmaceutical.getId()) {
                    listView.setItemChecked(i, true);
                }
            }
        dialog.show();
    }

    /**
     * Creates and displays dialog which supports multi selection.
     * The selection is visible in its linear layout afterwards.
     * <p/>
     * Positive button adds selection into linear layout.
     * Neutral button resets selection.
     * Negative button cancels dialog without change.
     * <p/>
     * Implementation for electrode locations.
     */
    private void selectElectrodeLocationsDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.base_list, null, false);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setAdapter(getElectrodeLocationsAdapter());

        if (!isWorking() && electrodeLocationAdapter.isEmpty())
            updateElectrodeLocations();

        dialog.setTitle(R.string.experiment_add_electrode_location);
        dialog.setView(dialogView);
        dialog.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                int len = listView.getCount();
                SparseBooleanArray checked = listView.getCheckedItemPositions();

                selectedElectrodeLocations.clear();

                //find out selected items
                for (int i = 0; i < len; i++) {
                    if (checked.get(i))
                        selectedElectrodeLocations.add(getElectrodeLocationsAdapter().getItem(i));
                }

                fillElectrodeLocationsRows();
            }
        });

        dialog.setNeutralButton(R.string.dialog_button_clear_selection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                selectedElectrodeLocations.clear();
                fillElectrodeLocationsRows();
                Toast.makeText(ExperimentAddActivity.this, R.string.dialog_selection_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        //reselect previously selected items
        for (ElectrodeLocation electrodeLocation : selectedElectrodeLocations)
            for (int i = 0; i < electrodeLocationAdapter.getCount(); i++) {
                if (electrodeLocationAdapter.getItem(i).getId() == electrodeLocation.getId()) {
                    listView.setItemChecked(i, true);
                }
            }
        dialog.show();
    }

    /* --------------------------------- Methods for filling linear layout with rows from multi-select spinners ------------------ */

    /**
     * Fills hardware list linear layout with hardware selection.
     */
    private void fillHardwareListRows() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.experiment_add_hardware_list);
        //clear previous values
        layout.removeAllViews();

        ExperimentDetailLists.fillHardwareList(layout, selectedHardware);
    }

    /**
     * Fills software list linear layout with software selection.
     */
    private void fillSoftwareListRows() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.experiment_add_software_list);
        //clear previous values
        layout.removeAllViews();

        ExperimentDetailLists.fillSoftwareList(layout, selectedSoftware);
    }

    /**
     * Fills disease list linear layout with disease selection.
     */
    private void fillDiseasesRows() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.experiment_add_disease_list);
        //clear previous values
        layout.removeAllViews();

        ExperimentDetailLists.fillDiseaseList(layout, selectedDiseases);
    }

    /**
     * Fills pharmaceuticals list linear layout with pharmaceuticals selection.
     */
    private void fillPharmaceuticalsRows() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.experiment_add_pharmaceutical_list);
        //clear previous values
        layout.removeAllViews();

        ExperimentDetailLists.fillPharmaceuticals(layout, selectedPharmaceuticals);
    }

    /**
     * Fills electrode location list linear layout with locations selection.
     */
    private void fillElectrodeLocationsRows() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.experiment_add_electrode_location_list);
        //clear previous values
        layout.removeAllViews();

        ExperimentDetailLists.fillElectrodeLocations(layout, selectedElectrodeLocations);
    }


    /**
     * Reads data from fields. If valid, returns filled instance, null otherwise.
     *
     * @return valid record or null
     */
    private Experiment getValidRecord() {

        //misc
        EditText temperature = (EditText) findViewById(R.id.experiment_add_temperature);
        EditText environmentNote = (EditText) findViewById(R.id.experiment_add_env_note);

        //single choice
        Spinner groupSpinner = (Spinner) findViewById(R.id.experiment_add_group);
        Spinner scenariosSpinner = (Spinner) findViewById(R.id.experiment_add_scenario);
        Spinner subjectsSpinner = (Spinner) findViewById(R.id.experiment_add_subject);
        Spinner artifactsSpinner = (Spinner) findViewById(R.id.experiment_add_artifact);
        Spinner digitizationsSpinner = (Spinner) findViewById(R.id.experiment_add_digitization);
        Spinner electrodeSystemsSpinner = (Spinner) findViewById(R.id.experiment_add_electrode_system);
        Spinner weatherList = (Spinner) findViewById(R.id.experiment_add_weather);

        ResearchGroup group = (ResearchGroup) groupSpinner.getSelectedItem();
        Scenario scenario = (Scenario) scenariosSpinner.getSelectedItem();
        Person subject = (Person) subjectsSpinner.getSelectedItem();
        Artifact artifact = (Artifact) artifactsSpinner.getSelectedItem();
        Digitization digitization = (Digitization) digitizationsSpinner.getSelectedItem();
        ElectrodeSystem system = (ElectrodeSystem) electrodeSystemsSpinner.getSelectedItem();
        Weather weather = (Weather) weatherList.getSelectedItem();

        //electrode
        EditText impedance = (EditText) findViewById(R.id.experiment_add_electrode_impedance);

        //weather

        StringBuilder error = new StringBuilder();
        //validations
        if (group == null)
            error.append(getString(R.string.error_no_group_selected)).append('\n');
        if (scenario == null)
            error.append(getString(R.string.error_no_scenario_selected)).append('\n');
        if (subject == null)
            error.append(getString(R.string.error_no_subject_selected)).append('\n');
        if (artifact == null)
            error.append(getString(R.string.error_no_artifact_selected)).append('\n');
        if (digitization == null)
            error.append(getString(R.string.error_no_digitization_selected)).append('\n');
        if (weather == null)
            error.append(getString(R.string.error_no_weather_selected)).append('\n');
        if (fromTime.getTime().after(toTime.getTime()))
            error.append(getString(R.string.error_time_start_after_end)).append('\n');
        if (ValidationUtils.isEmpty(impedance.getText().toString()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.experiment_electrode_impedance)).append(")").append('\n');
        if (system == null)
            error.append(getString(R.string.error_no_electrode_system_selected)).append('\n');
        if (selectedElectrodeLocations.isEmpty())
            error.append(getString(R.string.error_no_electrode_location_selected)).append('\n');

        if (error.toString().isEmpty()) {
            Experiment experiment = new Experiment();
            experiment.setResearchGroup(group);
            experiment.setArtifact(artifact);

            ScenarioSimple scenarioSimple = new ScenarioSimple();
            scenarioSimple.setScenarioId(scenario.getScenarioId());
            scenarioSimple.setScenarioName(scenario.getScenarioName());
            experiment.setScenario(scenarioSimple);

            Subject subj = new Subject();
            subj.setPersonId(subject.getId());
            subj.setName(subject.getName());
            subj.setSurname(subject.getSurname());
            subj.setGender(subject.getGender());
            experiment.setSubject(subj);
            experiment.setDigitization(digitization);

            String tmpTemp = temperature.getText().toString();
            experiment.setTemperature(tmpTemp.trim().isEmpty() ? 0 : Integer.parseInt(tmpTemp));
            experiment.setEnvironmentNote(environmentNote.getText().toString());

            ElectrodeConf conf = new ElectrodeConf();
            String tmpImpedance = impedance.getText().toString();
            conf.setImpedance(tmpImpedance.trim().isEmpty() ? 0 : Integer.parseInt(tmpImpedance));
            conf.setElectrodeSystem(system);
            conf.setElectrodeLocations(new ElectrodeLocationList(selectedElectrodeLocations));
            experiment.setElectrodeConf(conf);
            experiment.setDiseases(new DiseaseList(selectedDiseases));
            experiment.setPharmaceuticals(new PharmaceuticalList(selectedPharmaceuticals));
            experiment.setHardwareList(new HardwareList(selectedHardware));
            experiment.setSoftwareList(new SoftwareList(selectedSoftware));

            experiment.setWeather(weather);

            experiment.setStartTime(fromTime);
            experiment.setEndTime(toTime);

            return experiment;
        } else {
            showAlert(error.toString());
            return null;
        }
    }
}
