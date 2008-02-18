package org.apache.maps; 

import java.io.BufferedInputStream; 
import java.io.InputStream; 
import java.net.URL; 
import java.net.URLConnection; 
import android.location.Location;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.http.util.ByteArrayBuffer; 
import android.content.SharedPreferences;
import android.content.Context;

public class LocationObjects { 
	 public ArrayList<Friend> nearFriends = new ArrayList<Friend>(); // Array to store List of friends
	 protected Context aContext;
     public LocationObjects(int maxDistance, Context cContext) { 

    	   aContext = cContext;
	        // Save user preferences. We need an Editor object to
	       // make changes. All objects are from android.context.Context
	       SharedPreferences settings = aContext.getSharedPreferences(aContext.getString(R.string.app_name), 0);

		   // Restore preferences
	       String username = settings.getString("username", null);
	       String password = settings.getString("password", null);
    		       
          /* Login to server to get the Session ID */
          String sessionid = null;
          String myString = null; 
          try { 
               /* Define the URL we want to load data from. */ 
               URL myURL = new URL( 
                         "http://www.ipoki.com/signin.php?user="+username+"&pass="+password); 
               /* Open a connection to that URL. */ 
               URLConnection ucon = myURL.openConnection(); 

               /* Define InputStreams to read 
                * from the URLConnection. */ 
               InputStream is = ucon.getInputStream(); 
               BufferedInputStream bis = new BufferedInputStream(is);
                
               /* Read bytes to the Buffer until 
                * there is nothing more to read(-1). */ 
               ByteArrayBuffer baf = new ByteArrayBuffer(50); 
               int current = 0; 
               while((current = bis.read()) != -1){ 
                    baf.append((byte)current); 
               } 

               /* Convert the Bytes read to a String. */ 
               myString = new String(baf.toByteArray()); 
          } catch (Exception e) { 
               /* On any Error we want to display it. */ 
               myString = e.getMessage(); 
          } 

          StringTokenizer token = new StringTokenizer(myString, "$$$"); //use $$$ as a string delimiter
          String currentToken="";
          int i=0;
          while(token.hasMoreTokens()){
	          currentToken = token.nextToken();
	    	  i++;
	    	  if (i==2) {
	    		  sessionid = currentToken;
	    	  }
          }

          /* Take Objects List */ 
          myString = null; 
          try { 
              /* Define the URL we want to load data from. */ 
              URL myURL = new URL( 
                        "http://www.ipoki.com/myfriends.php?clave=" + sessionid); 
              /* Open a connection to that URL. */ 
              URLConnection ucon = myURL.openConnection(); 

              /* Define InputStreams to read 
               * from the URLConnection. */ 
              InputStream is = ucon.getInputStream(); 
              BufferedInputStream bis = new BufferedInputStream(is); 
               
              /* Read bytes to the Buffer until 
               * there is nothing more to read(-1). */ 
              ByteArrayBuffer baf = new ByteArrayBuffer(50); 
              int current = 0; 
              while((current = bis.read()) != -1){ 
                   baf.append((byte)current); 
              } 

              /* Convert the Bytes read to a String. */ 
              myString = new String(baf.toByteArray()); 
         } catch (Exception e) { 
              /* On any Error we want to display it. */ 
              myString = e.getMessage(); 
         } 

         // Loop through the objects list and save them in an array
         token = new StringTokenizer(myString, "$$$"); //use $$$ as a string delimiter
         currentToken="";
         i=0;
         String sUsername = null;
         String sSession = null;
         Location lLocation = null;
		 while(token.hasMoreTokens()){
		      currentToken = token.nextToken();
			  i++;
			  if (i==1) {
				  // username
				  sUsername = currentToken;
			  }
			  if (i==2) {
				  // latitude
				  lLocation = new Location();
				  lLocation.setLatitude(Float.parseFloat(currentToken));
			  }
			  if (i==3) {
				  // longitude
				  lLocation.setLongitude(Float.parseFloat(currentToken));
			  }
			  if (i==4) {
				  // sessionid
				  sSession = currentToken;
				  i=0;
				  this.nearFriends.add(new Friend(lLocation, sUsername, sSession));
			  }
         }
     } 
}
