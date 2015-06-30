package cz.zcu.kiv.eeg.mobile.base.localdb;

import android.content.Context;
import android.content.SharedPreferences;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.zcu.kiv.eeg.mobile.base.data.Values;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.PersonAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Artifact;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Owner;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Person;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Scenario;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Subject;
import cz.zcu.kiv.eeg.mobile.base.utils.ErrorChecker;

/**
 * Created by Isuru on 6/10/2015.
 */
public class FetchPersonDB {

    private Database database;
    private Context ctx;

    public FetchPersonDB(Database database, Context ctx){
        this.database = database;
        this.ctx = ctx;
    }

    public Owner FetchOwnerById(String docId ){

        Owner owner = new Owner();

        if( ! ErrorChecker.checkDb(ctx, database)){
            return new Owner();//empty
        }
        // retrieve the document from the database
        Document doc = database.getDocument(docId);

        Map<String, Object> docContent = doc.getProperties();

        owner.setId(doc.getId());
        owner.setName(docContent.get("name").toString());
        owner.setSurname(docContent.get("surname").toString());

        // return the object
        return owner;
    }

    public Subject FetchSubjectById(String docId ){

        Subject subject = new Subject();

        if( ! ErrorChecker.checkDb(ctx, database)){
            return new Subject();//empty
        }
        // retrieve the document from the database
        Document doc = database.getDocument(docId);

        Map<String, Object> docContent = doc.getProperties();

        subject.setPersonId(doc.getId());
        subject.setName(docContent.get("name").toString());
        subject.setSurname(docContent.get("surname").toString());
        subject.setGender(docContent.get("gender").toString());
        subject.setLeftHanded(Boolean.parseBoolean(docContent.get("lefthanded").toString()));

        // return the object
        return subject;
    }

    public void FetchPerson(String viewName, final String type, PersonAdapter personAdapter){

        SharedPreferences tempDataChk = ctx.getSharedPreferences(Values.PREFS_TEMP, Context.MODE_PRIVATE);
        String loggeduserDocID    = tempDataChk.getString("loggedUserDocID", null);

        View personView = database.getView(viewName);

        personView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get("type").equals(type)) {
                    HashMap<String, Object> value = new HashMap<String, Object>();
                    value.put("name", (String) document.get("name"));
                    emitter.emit(document.get("name"), value);
                }else{
//                    Toast.makeText(ctx, "No such document(s) in the database", Toast.LENGTH_SHORT).show();
                }
            }
        }, "1");

        Query query = database.getView(viewName).createQuery();
        List<Person> fetchedPersonList = new ArrayList<Person>();

        try {
            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                //Fetch all person records
                Person person = new Person();
                person.setId(row.getDocumentId().toString());
                person.setName(row.getDocument().getProperties().get("name").toString());
                person.setSurname(row.getDocument().getProperties().get("surname").toString());
                person.setBirthday(row.getDocument().getProperties().get("birthday").toString());
                person.setGender(row.getDocument().getProperties().get("gender").toString());
                person.setEmail(row.getDocument().getProperties().get("email").toString());
                person.setLeftHanded(row.getDocument().getProperties().get("lefthanded").toString());
                person.setNotes(row.getDocument().getProperties().get("notes").toString());
                person.setPhone(row.getDocument().getProperties().get("phone").toString());
                person.setDef_group_id(row.getDocument().getProperties().get("def_group_id").toString());

                fetchedPersonList.add(person);

            }

            personAdapter.clear();
            if (fetchedPersonList != null && !fetchedPersonList.isEmpty()){
                for (Person person : fetchedPersonList) {
                    personAdapter.add(person);
                }
            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }


}
