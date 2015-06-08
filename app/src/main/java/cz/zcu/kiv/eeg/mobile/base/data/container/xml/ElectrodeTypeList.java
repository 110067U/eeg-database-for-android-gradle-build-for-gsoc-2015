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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Data container for holding list of electrode types.
 *
 * @author Petr Miko
 */
@Root(name = "electrodeTypes")
public class ElectrodeTypeList implements Parcelable {

    public static final Creator<ElectrodeTypeList> CREATOR
            = new Creator<ElectrodeTypeList>() {
        public ElectrodeTypeList createFromParcel(Parcel in) {
            return new ElectrodeTypeList(in);
        }

        public ElectrodeTypeList[] newArray(int size) {
            return new ElectrodeTypeList[size];
        }
    };
    @ElementList(inline = true, required = false)
    private List<ElectrodeType> electrodeTypes;

    public ElectrodeTypeList() {
    }

    public ElectrodeTypeList(Parcel in) {
        electrodeTypes = new ArrayList<ElectrodeType>();
        in.readTypedList(electrodeTypes, ElectrodeType.CREATOR);
    }

    public List<ElectrodeType> getElectrodeTypes() {
        return electrodeTypes;
    }

    public void setElectrodeTypes(List<ElectrodeType> electrodeTypes) {
        this.electrodeTypes = electrodeTypes;
    }

    public boolean isAvailable() {
        return electrodeTypes != null && !electrodeTypes.isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(electrodeTypes);
    }
}
