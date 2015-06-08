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

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.SaveDiscardActivity;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Artifact;
import cz.zcu.kiv.eeg.mobile.base.utils.ConnectionUtils;
import cz.zcu.kiv.eeg.mobile.base.utils.LimitedTextWatcher;
import cz.zcu.kiv.eeg.mobile.base.utils.ValidationUtils;
import cz.zcu.kiv.eeg.mobile.base.ws.asynctask.CreateArtifact;

/**
 * Activity for creating new artifact record.
 *
 * @author Petr Miko
 */
public class ArtifactAddActivity extends SaveDiscardActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_artifact_add);

        initView();
    }

    /**
     * Initializes view elements.
     */
    private void initView() {

        final EditText compensation = (EditText) findViewById(R.id.experiment_artifact_compensation_add);
        final EditText rejectCondition = (EditText) findViewById(R.id.experiment_artifact_reject_add);
        TextView compensationCount = (TextView) findViewById(R.id.experiment_artifact_compensation_add_count);
        TextView rejectCount = (TextView) findViewById(R.id.experiment_artifact_reject_add_count);
        compensation.addTextChangedListener(new LimitedTextWatcher(getResources().getInteger(R.integer.limit_artifact), compensationCount));
        rejectCondition.addTextChangedListener(new LimitedTextWatcher(getResources().getInteger(R.integer.limit_artifact), rejectCount));
    }

    /**
     * Reads data from fields, if valid proceeds with creating new record on server.
     */
    @Override
    protected void save() {

        Artifact record;
        if ((record = getValidRecord()) != null) {
            if (ConnectionUtils.isOnline(this)) {
                new CreateArtifact(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, record);
            } else
                showAlert(getString(R.string.error_offline));
        }
    }

    /**
     * Returns valid record or null, if input values are not valid.
     *
     * @return valid record
     */
    private Artifact getValidRecord() {

        EditText compensation = (EditText) findViewById(R.id.experiment_artifact_compensation_add);
        EditText rejectCondition = (EditText) findViewById(R.id.experiment_artifact_reject_add);

        StringBuilder error = new StringBuilder();

        //validations
        if (ValidationUtils.isEmpty(compensation.getText().toString()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.experiment_artifact_compensation)).append(")").append('\n');
        if (ValidationUtils.isEmpty(rejectCondition.getText().toString()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.experiment_artifact_reject)).append(")").append('\n');

        //if no error, run service
        if (error.toString().isEmpty()) {
            Artifact record = new Artifact();
            record.setCompensation(compensation.getText().toString());
            record.setRejectCondition(rejectCondition.getText().toString());

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
