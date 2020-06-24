/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sps.data;
 
// Imports the Google Cloud client library
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GaeRequestHandler;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.TimeZoneApi;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/* Location object */
public class Location {
    private String address;
    private double latCoord;
    private double lngCoord;
    private LatLng coords;
    private TimeZone timeZoneObj;
    private String timeZoneID;

    public Location(String address) {
      this.address = address;
      try{
        setProperties();
      } catch (Exception e){
          e.printStackTrace();
      }
    }

    public void setProperties() throws Exception{
        GeoApiContext context = new GeoApiContext.Builder()
            .apiKey("PUTAPIKEYHERE")
            .build();

        // Synchronous
        try {
            getCoordinates(context, this.address);
            getTimeZone(context, this.coords);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getCoordinates(GeoApiContext context, String address) throws Exception{
        try {
            GeocodingResult[] results =  GeocodingApi.geocode(context,
             address).await();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            this.latCoord = results[0].geometry.location.lat;
            this.lngCoord = results[0].geometry.location.lng;
            this.coords = new LatLng(this.latCoord, this.lngCoord);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getTimeZone(GeoApiContext context, LatLng location) throws Exception{
        try {
            TimeZone results =  TimeZoneApi.getTimeZone(context,
             location).await();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            this.timeZoneObj = results;
            this.timeZoneID = results.getID();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAddress() {
      return this.address;
    }
 
    public Double getLat() {
      return this.latCoord;
    }

    public Double getLng() {
      return this.lngCoord;
    }

    public LatLng getCoords() {
      return this.coords;
    }
 
    public String getTimeZoneID() {
      return this.timeZoneID;
    }

    public TimeZone getTimeZone() {
      return this.timeZoneObj;
    }
}
