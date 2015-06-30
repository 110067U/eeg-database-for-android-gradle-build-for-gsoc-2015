package cz.zcu.kiv.eeg.mobile.base.localdb;

import android.content.Context;

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

import cz.zcu.kiv.eeg.mobile.base.data.adapter.ArtifactAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Artifact;
import cz.zcu.kiv.eeg.mobile.base.utils.ErrorChecker;

/**
 * Created by Isuru on 6/10/2015.
 */
public class FetchUserProfileDB {

    private Database database;
    private Context ctx;

    public FetchUserProfileDB(Database database, Context ctx){
        this.database = database;
        this.ctx = ctx;
    }

    public String FetchUserProfile(String viewName, final String type){

        View userProfileView = database.getView(viewName);

        userProfileView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get("type").equals(type)) {
                    HashMap<String, Object> value = new HashMap<String, Object>();
                    value.put("userId", (String) document.get("userId"));
                    emitter.emit(document.get("userName"), value);
                }else{
//                    Toast.makeText(ctx, "No such document(s) in the database", Toast.LENGTH_SHORT).show();
                }
            }
        }, "1");

        Query query = database.getView(viewName).createQuery();
        String loggedUserId = null;

        try {
            QueryEnumerator result = query.run();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                loggedUserId = row.getDocument().getProperties().get("userId").toString();
            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return loggedUserId;
    }//end of method

}
