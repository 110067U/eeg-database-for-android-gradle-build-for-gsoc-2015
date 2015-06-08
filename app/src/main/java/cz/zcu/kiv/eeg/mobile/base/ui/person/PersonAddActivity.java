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
package cz.zcu.kiv.eeg.mobile.base.ui.person;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.SaveDiscardActivity;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Person;
import cz.zcu.kiv.eeg.mobile.base.utils.ConnectionUtils;
import cz.zcu.kiv.eeg.mobile.base.utils.LimitedTextWatcher;
import cz.zcu.kiv.eeg.mobile.base.utils.ValidationUtils;
import cz.zcu.kiv.eeg.mobile.base.ws.asynctask.CreatePerson;

/**
 * Activity for creating new Person in eeg base.
 * Mainly meant fro creating new subjects.
 *
 * @author Petr Miko
 */
public class PersonAddActivity extends SaveDiscardActivity {

    private final static String DATE_PATTERN = "dd/MM/yyyy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_person_add);
        initView();
    }

    /**
     * Initialized layout view elements.
     */
    private void initView() {

        //getting layout elements
        final TextView notesCountText = (TextView) findViewById(R.id.person_notes_count);
        EditText descriptionText = (EditText) findViewById(R.id.person_notes_value);
        TextView birthday = (TextView) findViewById(R.id.person_birthday_label);

        //limiting description text length
        descriptionText.addTextChangedListener(new LimitedTextWatcher(getResources().getInteger(R.integer.limit_description_chars), notesCountText));

        //informing about required date pattern
        birthday.setText(birthday.getText().toString() + " (" + DATE_PATTERN + ")");
    }

    @Override
    protected void save() {
        // getting layout elements
        EditText name = (EditText) findViewById(R.id.person_name_value);
        EditText surname = (EditText) findViewById(R.id.person_surname_value);
        EditText mail = (EditText) findViewById(R.id.person_mail_value);
        Spinner gender = (Spinner) findViewById(R.id.person_gender_value);
        EditText birthday = (EditText) findViewById(R.id.person_birthday_value);
        CompoundButton leftHanded = (CompoundButton) findViewById(R.id.person_lefthand_value);
        EditText notes = (EditText) findViewById(R.id.person_notes_value);
        EditText phone = (EditText) findViewById(R.id.person_phone_value);

        //creating person instance
        Person person = new Person();
        person.setName(name.getText().toString());
        person.setSurname(surname.getText().toString());
        person.setEmail(mail.getText().toString());
        person.setGender(gender.getSelectedItemPosition() == 0 ? "M" : "Y");
        person.setBirthday(birthday.getText().toString());
        person.setNotes(notes.getText().toString());
        person.setLeftHanded(leftHanded.isChecked() ? "L" : "R");
        person.setPhone(phone.getText().toString());

        validateAndRun(person);
    }

    /**
     * Validates whether provided person information are correct.
     * If so, CreatePerson service is invoked.
     *
     * @param person person to be created.
     */
    private void validateAndRun(Person person) {

        if (!ConnectionUtils.isOnline(this)) {
            showAlert(getString(R.string.error_offline));
            return;
        }

        StringBuilder error = new StringBuilder();

        //validations
        if (ValidationUtils.isEmpty(person.getName()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.person_name)).append(")").append('\n');
        if (ValidationUtils.isEmpty(person.getSurname()))
            error.append(getString(R.string.error_empty_field)).append(" (").append(getString(R.string.person_surname)).append(")").append('\n');
        if (!ValidationUtils.isEmailValid(person.getEmail()))
            error.append(getString(R.string.error_invalid_mail_format)).append('\n');
        if (!ValidationUtils.isDateValid(person.getBirthday(), DATE_PATTERN))
            error.append(getString(R.string.error_invalid_date_format)).append(" (").append(DATE_PATTERN).append(")").append('\n');

        //if no error, run service
        if (error.toString().isEmpty()) {
            new CreatePerson(this).execute(person);
        } else {
            showAlert(error.toString());
        }
    }

    @Override
    protected void discard() {
        finish();
    }
}
