package cz.zcu.kiv.eeg.mobile.base.localdb;

import android.content.Context;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ArtifactAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.DigitizationAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.DiseaseAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ElectrodeFixAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ElectrodeLocationAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ElectrodeSystemAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ElectrodeTypeAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ExperimentAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.HardwareAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.PersonAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.PharmaceuticalAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ResearchGroupAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ScenarioAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.SoftwareAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.WeatherAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Artifact;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Digitization;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Disease;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.ElectrodeLocation;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.ElectrodeSystem;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Hardware;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Owner;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Pharmaceutical;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.ResearchGroup;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.ScenarioSimple;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Software;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Subject;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Weather;
import cz.zcu.kiv.eeg.mobile.base.utils.ErrorChecker;


/**
 * Created by Isuru on 6/7/2015.
 * CouchBase Lite database wrapper
 *
 */

public class CBDatabase {

    private Database database;
    private Context ctx;
    private Manager manager;
    // keep a reference to a running replication
    private Replication replica;


    /**
     * @param dbname
     * @param context
     */
    public CBDatabase(String dbname, Context context){

        this.ctx = context;
		/* Manages access to databases */
        try {
            manager = new Manager( new AndroidContext(ctx), Manager.DEFAULT_OPTIONS );
        }
        catch (IOException e) {
            ErrorChecker.ShowException(ctx, R.string.err_create_manager, e);
            return;
        }
        // create a name for the database and make sure the name is legal
        if ( ! Manager.isValidDatabaseName(dbname)) {
            ErrorChecker.showError( ctx, R.string.err_db_name );
            return;
        }
        // get existing db with that name
        // or create a new one if it doesn't exist
        try {
            database = manager.getDatabase(dbname);
        }
        catch (CouchbaseLiteException e) {
            ErrorChecker.ShowException(ctx, R.string.err_no_db, e );
            return;
        }
    }

    public Database getDatabase(){
        return database;
    }

    /** Release all resources and close all Databases. */
    public void close(){
        if(manager != null){
            manager.close();
        }
    }

//	/* Replication *********************************/
//
//    /**
//     * Create a push/pull Replica that is continuous/one-shot
//     * @param remote URL (Server or P2P)
//     * @param push: local DB --> remote DB
//     *        pull: remote DB --> local DB
//     * @param continuous: stay active indefinitely,
//     *        one-shot: transfer all changes, then quit.
//     * @return
//     */
//    public Replication startReplica( URL remote, Keys.Replica pushOrPull, Keys.Span span){
//
//        replica = (pushOrPull == Keys.Replica.PUSH)
//                ? database.createPushReplication(remote)
//                : database.createPullReplication(remote);
//
//        replica.setContinuous(span == Keys.Span.CONTINUOUS ? true : false);
//        replica.start();//a replication runs asynchronously
//
//        return replica;
//    }
//
//    /**
//     * A replica can be active/stopped/off-line/idle
//     * @param rep
//     * @return
//     */
//    public boolean isReplicaActive( Replication rep){
//        return rep != null && (rep.getStatus() ==
//                Replication.ReplicationStatus.REPLICATION_ACTIVE);
//    }

    //Retrive user profile data

    public String fetchUserProfileData(){
        String loggedUserId = null;

        loggedUserId = new FetchUserProfileDB(database,ctx).FetchUserProfile("fetchUserData","Profile");

        return loggedUserId;
    }


    //Retrieve data using views when we want to fetch specific data
    public void createScenarioView(String viewName, final String type, ScenarioAdapter scenarioAdapter){
        if(viewName.equals("fetchMyScenariosView")){
            new FetchScenariosDB(database,ctx).FetchMyScenarios(viewName, type, scenarioAdapter);
        }else if(viewName.equals("fetchAllScenariosView")){
            new FetchScenariosDB(database,ctx).FetchAllScenarios(viewName,type,scenarioAdapter);
        }
    }

    public ScenarioSimple fetchSimpleScenarioById(String docId){
        return new FetchScenariosDB(database,ctx).FetchSimpleScenarioById(docId);
    }

    public void createResearchGroupView(String viewName, final String type, ResearchGroupAdapter researchGroupAdapter){
        if(viewName.equals("fetchResearchGroupsView")){
            new FetchResearchGroupsDB(database,ctx).FetchAllResearchGroups(viewName, type, researchGroupAdapter);
        }
    }

    public ResearchGroup fetchRgById(String docId){
        return new FetchResearchGroupsDB(database,ctx).FetchResearchGroupById(docId);
    }


    public String fetchDefaultResearchGroup(String loggedUserId){
        String defResGrpId = new FetchResearchGroupsDB(database,ctx).FetchMyDefaultResearchGroupId("defResGrp","Group",loggedUserId);
        return  defResGrpId;
    }


    public void createPersonView(String viewName, final String type, PersonAdapter personAdapter){
        if(viewName.equals("fetchPersonView")){
            new FetchPersonDB(database,ctx).FetchPerson(viewName, type, personAdapter);
        }
    }

    public Subject fetchSubjectById(String docId){
        return new FetchPersonDB(database,ctx).FetchSubjectById(docId);
    }

    public Owner fetchOwnerById(String docId){
        return new FetchPersonDB(database,ctx).FetchOwnerById(docId);
    }

    public void createArtifactView(String viewName, final String type, ArtifactAdapter artifactAdapter){
        if(viewName.equals("fetchAllArtifactView")){
            new FetchArtifactsDB(database,ctx).FetchAllArtifacts(viewName, type, artifactAdapter);
        }
    }

    public Artifact fetchArtifactById(String docId){
        return new FetchArtifactsDB(database,ctx).FetchArtifactById(docId);
    }

    public void createDigitizationView(String viewName, final String type, DigitizationAdapter digitizationAdapter){
        if(viewName.equals("fetchAllDigitizationView")){
            new FetchDigitizationsDB(database,ctx).FetchAllDigitizations(viewName, type, digitizationAdapter);
        }
    }

    public Digitization fetchDigitizationById(String docId){
        return new FetchDigitizationsDB(database,ctx).FetchDigitizationById(docId);
    }

    public void createHardwareView(String viewName, final String type, HardwareAdapter hardwareAdapter){
        if(viewName.equals("fetchAllHardwareView")){
            new FetchHardwareListDB(database,ctx).FetchAllHardware(viewName, type, hardwareAdapter);
        }
    }

    public Hardware fetchHardwareById(String docId){
        return new FetchHardwareListDB(database,ctx).FetchHardwareById(docId);
    }

    public void createSoftwareView(String viewName, final String type, SoftwareAdapter softwareAdapter){
        if(viewName.equals("fetchAllSoftwareView")){
            new FetchSoftwareListDB(database,ctx).FetchAllSoftware(viewName, type, softwareAdapter);
        }
    }

    public Software fetchSoftwareById(String docId){
        return new FetchSoftwareListDB(database,ctx).FetchSoftwareById(docId);
    }

    public void createDiseasesView(String viewName, final String type, DiseaseAdapter diseaseAdapter){
        if(viewName.equals("fetchAllDiseasesView")){
            new FetchDiseaseListDB(database,ctx).FetchAllDiseases(viewName, type, diseaseAdapter);
        }
    }

    public Disease fetchDiseaseById(String docId){
        return new FetchDiseaseListDB(database,ctx).FetchDiseaseById(docId);
    }

    public void createPharmaceuticalsView(String viewName, final String type, PharmaceuticalAdapter pharmaceuticalAdapter){
        if(viewName.equals("fetchAllPharmaceuticalsView")){
            new FetchPharmaceuticalsListDB(database,ctx).FetchAllPharmaceuticals(viewName, type, pharmaceuticalAdapter);
        }
    }

    public Pharmaceutical fetchPharmaceuticalById(String docId){
        return new FetchPharmaceuticalsListDB(database,ctx).FetchPharmaceuticalById(docId);
    }


    public void createWeatherView(String viewName, final String type, WeatherAdapter weatherAdapter, String resGroupId){
        if(viewName.equals("fetchWeatherRecordsView")){
            new FetchWeatherListDB(database,ctx).FetchWeatherRecords(viewName, type, weatherAdapter, resGroupId);
        }
    }

    public Weather fetchWeatherById(String docId){
        return new FetchWeatherListDB(database,ctx).FetchWeatherById(docId);
    }

    public void createElectrodeSystemView(String viewName, final String type, ElectrodeSystemAdapter electrodeSystemAdapter){
        if(viewName.equals("fetchAllElectrodeSystemRecordsView")){
            new FetchElectrodeSystemsListDB(database,ctx).FetchAllElectrodeSystemRecords(viewName, type, electrodeSystemAdapter);
        }
    }

    public ElectrodeSystem fetchElectrodeSystemById(String docId){
        return new FetchElectrodeSystemsListDB(database,ctx).FetchElectrodeSystemById(docId);
    }

    public void createElectrodeLocationView(String viewName, final String type, ElectrodeLocationAdapter electrodeLocationAdapter){
        if(viewName.equals("fetchAllElectrodeLocationRecordsView")){
            new FetchElectrodeLocationsListDB(database,ctx).FetchAllElectrodeLocations(viewName, type, electrodeLocationAdapter);
        }
    }

    public ElectrodeLocation fetchElectrodeLocationById(String docId){
        return new FetchElectrodeLocationsListDB(database,ctx).FetchElectrodeLocationById(docId);
    }

    public void createElectrodeFixesView(String viewName, final String type, ElectrodeFixAdapter electrodeFixAdapter){
        if(viewName.equals("fetchAllElectrodeFixRecordsView")){
            new FetchElectrodeFixesListDB(database,ctx).FetchAllElectrodeFixRecords(viewName, type, electrodeFixAdapter);
        }
    }

    public void createElectrodeTypesView(String viewName, final String type, ElectrodeTypeAdapter electrodeTypeAdapter){
        if(viewName.equals("fetchAllElectrodeTypeRecordsView")){
            new FetchElectrodeTypesListDB(database,ctx).FetchAllElectrodeTypeRecords(viewName, type, electrodeTypeAdapter);
        }
    }

    public void createExperimentView(String viewName, final String type, ExperimentAdapter experimentAdapter){
        if(viewName.equals("fetchMyExperimentsView")){
            new FetchExperimentsDB(database,ctx).FetchMyExperiments(viewName, type, experimentAdapter);
        }
    }

	/* CRUD Operations *********************************/
    /**
     * Create documents
     * @param docContent
     * @return docId
     */
    public String create( Map<String, Object> docContent ){

        if( ! ErrorChecker.checkDb(ctx, database)){
            return "";
        }
        // create an empty document
        Document doc = database.createDocument();

        // add content to document and write the document to the database
        try {
            doc.putProperties(docContent);
        }catch (CouchbaseLiteException e) {
            ErrorChecker.ShowException(ctx, R.string.err_db_write, e ) ;
            return "";
        }
        return doc.getId();
    }

    /**
     * Read / Retrive from document ID
     * @param docId
     * @return Doc content
     */
    public Map<String, Object> retrieve(String docId){

        if( ! ErrorChecker.checkDb(ctx, database)){
            return new HashMap<String, Object>();//empty
        }
        // retrieve the document from the database
        Document doc = database.getDocument(docId);

        // display the retrieved document
        return doc.getProperties();
    }


    /**
     * Update
     * @param key
     * @param value
     * @param docId
     * @return success or failure
     */
    public boolean update( final String key, final Object value, String docId ){

        if( ! ErrorChecker.checkDb(ctx, database)){
            return false;
        }
        // update the document
        try {
            Document doc = database.getDocument(docId);

            // this alternative way is better for handling write conflicts
            doc.update(new Document.DocumentUpdater() {
                @Override
                public boolean update(UnsavedRevision newRevision) {
                    Map<String, Object> properties = newRevision.getUserProperties();
                    properties.put(key, value);
                    newRevision.setUserProperties(properties);
                    return true;
                }
            });

		/*	Map<String, Object> docContent = doc.getProperties();
			//Working on a copy
			Map<String, Object> updatedContent = new HashMap<String, Object>();
			updatedContent.putAll(docContent);
			updatedContent.put(key, value);
			doc.putProperties(updatedContent);*/
        }
        catch (CouchbaseLiteException e) {
            ErrorChecker.ShowException(ctx, R.string.err_db_update, e ) ;
            return false;
        }
        return true;
    }


    /**
     * Delete
     * @param docId
     * @return
     */
    public boolean delete(String docId){

        if( ! ErrorChecker.checkDb(ctx, database)){
            return false;
        }
        Document doc = null;
        // delete the document
        try {
            doc = database.getDocument(docId);
            doc.delete();
        }
        catch (CouchbaseLiteException e) {
            ErrorChecker.ShowException(ctx, R.string.err_db_delete, e ) ;
        }
        return  doc.isDeleted();
    }

}
