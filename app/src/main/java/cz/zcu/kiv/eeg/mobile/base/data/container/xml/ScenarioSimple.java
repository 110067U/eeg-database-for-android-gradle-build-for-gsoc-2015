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
package cz.zcu.kiv.eeg.mobile.base.data.container.xml;

import android.os.Parcel;
import android.os.Parcelable;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Data container of basic information about scenario.
 * Support XML marshaling and is parcelable.
 *
 * @author Petr Miko
 */
@Root(name = "scenario")
public class ScenarioSimple implements Parcelable {

    public static final Parcelable.Creator<ScenarioSimple> CREATOR
            = new Parcelable.Creator<ScenarioSimple>() {
        public ScenarioSimple createFromParcel(Parcel in) {
            return new ScenarioSimple(in);
        }

        public ScenarioSimple[] newArray(int size) {
            return new ScenarioSimple[size];
        }
    };
    @Element
    private int scenarioId;
    @Element
    private String scenarioName;

    public ScenarioSimple() {
    }

    public ScenarioSimple(Parcel in) {
        scenarioId = in.readInt();
        scenarioName = in.readString();
    }

    public int getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(int scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(scenarioId);
        dest.writeString(scenarioName);
    }
}
