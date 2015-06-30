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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.SaveDiscardActivity;
import cz.zcu.kiv.eeg.mobile.base.data.Values;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.ElectrodeFix;
import cz.zcu.kiv.eeg.mobile.base.localdb.CBDatabase;
import cz.zcu.kiv.eeg.mobile.base.utils.ConnectionUtils;
import cz.zcu.kiv.eeg.mobile.base.utils.Keys;
import cz.zcu.kiv.eeg.mobile.base.utils.LimitedTextWatcher;
import cz.zcu.kiv.eeg.mobile.base.utils.ValidationUtils;
import cz.zcu.kiv.eeg.mobile.base.ws.asynctask.CreateElectrodeFix;

/**
 * Activity for creating new electrode fix record.
 *
 * @author Petr Miko
 */
public class ElectrodeFixAddActivity extends SaveDiscardActivity {

    private String default_researchGroupId;
    private CBDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_electrode_fix_add);
        default_researchGroupId = getIntent().getStringExtra("groupId");
        initView();
    }

    /**
     * Initializes view elements.
     */
    private void initView() {

        EditText fixDescription = (EditText) findViewById(R.id.electrode_add_fix_description);
        TextView fixDescriptionCount = (TextView) findViewById(R.id.electrode_add_fix_description_count);
        fixDescription.addTextChangedListener(new LimitedTextWatcher(getResources().getInteger(R.integer.limit_description_chars), fixDescriptionCount));
    }

    /**
     * Reads data from fields, if valid proceeds with creating new record on server.
     */
    @Override
    protected void save() {

        ElectrodeFix record;
        if ((record = getValidRecord()) != null) {

//===================================================================Database Code==================================================//
            db = new CBDatabase(Keys.DB_NAME, ElectrodeFixAddActivity.this);
            // create an object that contains data for a document
            Map<String, Object> docContent = new HashMap<String, Object>();
            docContent.put("type", "ElectrodeFix");
            docContent.put("title",record.getTitle());
            docContent.put("description", record.getDescription());
            docContent.put("default_number", record.getDefaultNumber());
            docContent.put("def_group_id", default_researchGroupId); //create an attribute to make a relationship (Belongs to) with "ResearchGroup" entity

            String electrodeFixID = null;
            try {
                // Create a new document
                electrodeFixID = db.create(docContent);
                assert(electrodeFixID != null);
                Intent resultIntent = new Intent();
                record.setId(electrodeFixID);
                resultIntent.putExtra(Values.ADD_ELECTRODE_FIX_KEY, record);
                Toast.makeText(ElectrodeFixAddActivity.this, R.string.creation_ok, Toast.LENGTH_SHORT).show();
                ElectrodeFixAddActivity.this.setResult(Activity.RESULT_OK, resultIntent);
                ElectrodeFixAddActivity.this.finish();
            }
            catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ElectrodeFixAddActivity.this, R.string.creation_failed, Toast.LENGTH_SHORT).show();
            }
// ================================================================================================================================//


//            if (ConnectionUtils.isOnline(this)) {
//                new CreateElectrodeFix(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, record);
//            } else
//                showAlert(getString(R.string.error_offline));
        }
    }

    /**
     * Returns valid record or null, if input values are not valid.
     *
     * @return valid record
     */
    private ElectrodeFix getValidRecord() {

        EditText fixTitle = (EditText) findViewById(R.id.electrode_add_fix_title);
        EditText fixDescription = (EditText) findViewById(R.id.electrode_add_fix_description);

        StringBuilder error = new StringBuilder();

        //validations
        if (ValidationUtils.isEmpty(fixTitle.getText().toString()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.dialog_title)).append(")").append('\n');
        if (ValidationUtils.isEmpty(fixDescription.getText().toString()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.dialog_description)).append(")").append('\n');

        //if no error, run service
        if (error.toString().isEmpty()) {
            ElectrodeFix record = new ElectrodeFix();
            record.setTitle(fixTitle.getText().toString());
            record.setDescription(fixDescription.getText().toString());

            return record;
        } else {
            showAlert(error.toString());
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void discard() {
        finish();
    }
}
