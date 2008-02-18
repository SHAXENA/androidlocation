package org.apache.maps;

import android.location.Location;

/**
 * Small class to combine 
 * a Name and a Location.
 * Hit me, I didn't use Getters & Setters. ;) 
 */
public class Friend{
	public Location itsLocation = null;
	public String itsUsername = null;
	public String itsSession = null;
	public Friend(Location aLocation, String aName, String aSession){
		this.itsLocation = aLocation;
		this.itsUsername = aName;
		this.itsSession = aSession;
	}
}
