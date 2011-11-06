package nl.michiel1972.main;

/*
 * Copyright (C) 2011 M.Minderhoud <michiel1972@gmail.com>

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to

Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor
Boston, MA   02110-1301, USA.

 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.login.LoginException;

import nl.michiel1972.main.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UploadToCommonsActivity extends Activity {
	
	
	private Bundle extras;
	private EditText edittext1;
	private EditText edittext2;
	private EditText edittext3;
	private EditText edittext4;
	private EditText edittext5;
	private Button buttonStart;
	private Button buttonCancel;
	private Uri uri;
	private ProgressDialog mProgressDialog;
	
	private String username="";
	private String password="";
	private String filenameprefix="";
	private String categoryname="";
	private String filename="";
	
	private String detailinfo="";
	private String licenseinfo="";
	private String imageDescription="";
	private String exif_datetime="";
	private Wiki theWiki;
	private byte[] data;
	private boolean doOverwrite=false;


	private String endingMessage="";
	
	public String getEndingMessage() {
		return endingMessage;
	}

	public void setEndingMessage(String endingMessage) {
		this.endingMessage = endingMessage;
	}

	/*
	 * AsyncTask enables proper and easy use of the UI thread.
	 * This class allows to perform background operations and publish results on the UI thread
	 * without having to manipulate threads and/or handlers.
	 * An asynchronous task is defined by a computation that runs on a background thread and whose result is published on the UI thread. An asynchronous task is defined by 3 generic types, called Params, Progress and Result, and 4 steps, called onPreExecute, doInBackground, onProgressUpdate and onPostExecute.
	 */
	private class UploadImageTask extends AsyncTask<Void, Void, Void> {

	
		@Override
		protected void onPreExecute() {

			// Query gallery for camera picture via
			// Android ContentResolver interface
			ContentResolver cr = getContentResolver();
			InputStream is = getInputStreamImage(uri, cr);
			
			exif_datetime = getFormatedEXIFdate(uri);
			
			getUserInputdata();
			checkUpdatedEdits();
			

			mProgressDialog = ProgressDialog.show(UploadToCommonsActivity.this, "",
					"Please wait...", true);
		}

		/**
		 * Get input stream image
		 * @param uri
		 * @param cr
		 * @return
		 */
		private InputStream getInputStreamImage(Uri uri, ContentResolver cr) {
			InputStream is = null;
			try {
				is = cr.openInputStream(uri);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Get binary bytes for encode of image file
			try {
				data = getBytesFromFile(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return is;
		}

		/**
		 * Get formated EXIF data
		 * @param uri
		 * @return
		 */
		private String getFormatedEXIFdate(Uri uri) {
			// Get Exif date, convert into correct format
			String imagefile = getRealPathFromURI(uri);
		    ExifInterface exifInterface = null;
			try {
				exifInterface = new ExifInterface(imagefile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			exif_datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
			
			SimpleDateFormat dateParser = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			SimpleDateFormat dateConverter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date d = null;
			try {
				d = dateParser.parse(exif_datetime);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return dateConverter.format(d);
		}

	    public void onProgressUpdate(String... args){
	    	// nicelyEndApp(args[0]);
	    	Toast.makeText(UploadToCommonsActivity.this, args[0], Toast.LENGTH_LONG).show();
	    }
	    
	    @Override
	    protected void onPostExecute(Void result) {
	    	 mProgressDialog.dismiss();
	     	 Toast.makeText(UploadToCommonsActivity.this, endingMessage, Toast.LENGTH_LONG).show();
	     	 buttonStart.setClickable(true);
	     	 endingMessage="";
	    }
	    
		@Override
		//In background do the upload
		protected Void doInBackground(Void... params) {
			startProcessingUpload(this);
			return null;
		}


	 }
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		    MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.menu, menu);
		    return true;
		
	}
	
	/**
	 * Give \ suggest filename
	 * @return
	 */
	public String suggestFilename() {
		
		//Get latest username entry
		username = edittext1.getText().toString().trim();
		String filename_suggestion =  username+"_"+uri.getLastPathSegment()+".jpg";
		
		//if user did set a default prexix
		if (filenameprefix.length()>2) {
			filename_suggestion =  filenameprefix+"_"+uri.getLastPathSegment()+".jpg";
			
		}
		edittext3.setText(filename_suggestion);
		return filename_suggestion;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.fileprefix:
	        changeDefaultFilePrefix();
	        return true;
	    case R.id.category:
	        changeDefaultCategory();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	 	
	/**
	 * MenuOption: Change default file prefix
	 */
	private void changeDefaultFilePrefix() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Default filename prefix");
		alert.setMessage("Enter a default filename prefix for your future image uploads. The suffix with picture number and extension will be added automatically.");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setText(filenameprefix);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString().trim();
		  // Do something with value!
		  if (value.length()>2) {
			  filenameprefix = value.trim();
			  String corString;
			  corString=filenameprefix.replace(':','_');filenameprefix=corString;
			  corString=filenameprefix.replace('?','_');filenameprefix=corString;
			  corString=filenameprefix.replace('\\','_');filenameprefix=corString;
			  corString=filenameprefix.replace('/','_');filenameprefix=corString;
			  corString=filenameprefix.replace('*','_');filenameprefix=corString;
			  corString=filenameprefix.replace('%','_');filenameprefix=corString;;
			  corString=filenameprefix.replace('"','_');filenameprefix=corString;
			  corString=filenameprefix.replace('|','_');filenameprefix=corString;
			  corString=filenameprefix.replace('<','_');filenameprefix=corString;
			  corString=filenameprefix.replace('>','_');filenameprefix=corString;
			  SavePreferences("MEM11", filenameprefix);
			  suggestFilename();
		  } else {
			  
		   }
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
		
	}


	/**
	 * MenuOption: Change default category
	 */
	private void changeDefaultCategory() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Default category");
		alert.setMessage("Enter a default category name for your future image uploads. Wiki syntax is added automatically.");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setText(categoryname);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString().trim();
		  // Do something with value!
		  if (value.length()>2) {
			  categoryname = value;
			  SavePreferences("MEM12", value);
			  
		  } else {
			  
		   }
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
		
	}
	
	/* Called when the activity is first created. This is shown only once with empty parameters 
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		String action = intent.getAction();

 		
		// if this is from the share menu
		if (Intent.ACTION_SEND.equals(action))
		{
			setContentView(R.layout.main); 

			this.extras = extras;
			if (extras.containsKey(Intent.EXTRA_STREAM))
			{
				try
				{
					// Hide standard keyboard
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				
					// Get input field locations
					edittext1 = (EditText) this.findViewById(R.id.editText1);
					edittext2 = (EditText) this.findViewById(R.id.editText2);
					edittext3 = (EditText) this.findViewById(R.id.editText3);
					edittext4 = (EditText) this.findViewById(R.id.editText4);
					edittext5 = (EditText) this.findViewById(R.id.editText5);
					buttonStart = (Button) findViewById(R.id.button1);
					buttonCancel = (Button) findViewById(R.id.button2);
					
					
					// Get resource path from intent
					uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
					
					// Get user values previous known
					loadPreferences();

					suggestFilename();
					
					// Action starts after BUTTON click. Settings can not be changed anymore.
					
			         buttonStart.setOnClickListener(new View.OnClickListener() {
			             public void onClick(View v) {
			            	 
			                 // Perform action on click
			            	 
			            	 buttonStart.setClickable(false);
			           
							 new UploadImageTask().execute();
						
			             }


			         });
			         buttonCancel.setOnClickListener(new View.OnClickListener() {
			             public void onClick(View v) {
			            	 
			                 // Perform action on click
			            	 
			            	 nicelyEndApp("Cancelled upload to Wikimedia Commons");
						
			             }

			         });
				        
					return;
				} catch (Exception e)
				{
					Log.e(this.getClass().getName(), e.toString());
				}

			} else if (extras.containsKey(Intent.EXTRA_TEXT))
			{
				 nicelyEndApp("Text can not be 'Shared' ");
					
			}
		} else {
			//started at init or other
			 nicelyEndApp("Use the 'Share via' menu when viewing an image to use the upload function");
		}

	}

    /**
     * End app with message
     * @param message
     */
    public void nicelyEndApp(String message){
		 Toast.makeText(this,message, Toast.LENGTH_LONG).show();
 		 finish();
    }
    
    
    /**
     * Get real path from Uri
     * @param contentUri
     * @return
     */
    
    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
        

    /**
     * Returns the contents of the file in a byte array
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] getBytesFromFile(InputStream is) throws IOException {
    
        // Get the size of the file
        long length = is.available();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file ");
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
    
    /**
     * Save settings locally
     * @param key
     * @param value
     */
    private void SavePreferences(String key, String value){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
       }
    
    /**
     * Load settings locally once at startup
     */
    private void loadPreferences(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String strSavedMem1 = sharedPreferences.getString("MEM1", "unknown");
        String strSavedMem2 = sharedPreferences.getString("MEM2", "unknown");
        String strSavedMem5 = sharedPreferences.getString("MEM5", "");
        String strSavedMem11 = sharedPreferences.getString("MEM11", "");
        String strSavedMem12 = sharedPreferences.getString("MEM12", "");
        filenameprefix = strSavedMem11;
        categoryname = strSavedMem12; 
  
        //Give found preferences ONLY if value was defined
        if (!strSavedMem1.equals("unknown")) edittext1.setText(strSavedMem1);
        if (!strSavedMem2.equals("unknown")) edittext2.setText(strSavedMem2);
        if (strSavedMem5.equals("")) {
        	edittext5.setText("{{self|cc-by-sa-3.0|GFDL}}");
             } else {
            	 edittext5.setText(strSavedMem5);
             }
           
       }
    
    private boolean haveNetworkConnection()
    {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo)
        {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
    
    /**
     * Start uploading, start button click called from asynch task
     * @param uploadImageTask 
     * @throws LoginException 
     */
	protected void startProcessingUpload(UploadImageTask uploadImageTask) {
		
		theWiki= new Wiki(this);
		
		//Connected?
		boolean succesConnection = haveNetworkConnection();
		if (!succesConnection) {
			endingMessage="No internet connection";
			return;
				}
			
			
		//Try Login
		boolean succesLogin = false;
		try {
			succesLogin = theWiki.login(username, password);
		} catch (IOException e) {
			
			//nicelyEndApp("Login error");
			e.printStackTrace();
		}
			
		
		if (succesLogin) {
			Log.i(this.getClass().getName(), "login ok");
			
						
			//upload file using bytes
			String newString = filename.replaceAll("\\s+", "_");
			filename=newString;
			try {
				theWiki.uploadAndroid(data, 
				        filename, 
				        imageDescription, 
				        "");
			} catch (LoginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		} else {
			Log.i(this.getClass().getName(), "login failed");
		}
				
	    
		
	}


	/*
	 * Get user data input used for description of file upload
	 */
	private void getUserInputdata() {
        //Get user data (data may be edited after suggestions at startup)
		username = edittext1.getText().toString().trim();
		password = edittext2.getText().toString().trim();
		filename = edittext3.getText().toString().trim();
		detailinfo = edittext4.getText().toString().trim();		
		licenseinfo = edittext5.getText().toString().trim();
		SavePreferences("MEM5", licenseinfo);
		String categorystring="";
		if (categoryname.length()>2) {
			categorystring= "[[Category:"+categoryname+"]]";
		}
		
        //Complete contents description
		imageDescription= getImageDescription(username,filename,detailinfo,licenseinfo, categorystring);
	}

	/**
	 * Check if login is changed and update if needed
	 */
	private void checkUpdatedEdits() {
		
		//button click implies user thinks text fields are correctly updated
		SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String strSavedMem1 = sharedPreferences.getString("MEM1", "unknown");
        String strSavedMem2 = sharedPreferences.getString("MEM2", "unknown");
		
        if ((!username.equals(strSavedMem1)) || (!password.equals(strSavedMem2))) { 
			 Toast.makeText(this,"Login data updated", Toast.LENGTH_SHORT).show();
		         
		     SavePreferences("MEM1", username);
		     SavePreferences("MEM2", password);
		     SavePreferences("MEM5", licenseinfo);
		}
		
	}

	/**
	 * Get and construct image description wikipedia formated
	 * @param username2
	 * @param filename2
	 * @param detailinfo2
	 * @param licenseinfo2
	 * @return
	 */
	private String getImageDescription(String username2, String filename2,
			String detailinfo2, String licenseinfo2, String categorystring) {
		      return "==Summary==\n" +
				  "{{Information\n" +
					"|Description= " + detailinfo + "\n" +
					"|Source= {{own}}\n" + 
					"|Date= " + exif_datetime + "\n" +
					"|Author= [[User:" +username +"|"+ username + "]]\n" +
					"|Permission= see below\n" +
					"|other_versions=\n" +
					"}}\n" +
					"{{Images uploaded with Android}}\n" +
					"==Licensing==\n" +
					licenseinfo +"\n" +
					categorystring
					;
	}

	/**
	 * @return the doOverwrite
	 */
	public boolean isDoOverwrite() {
		return doOverwrite;
	}

	/**
	 * @param doOverwrite the doOverwrite to set
	 */
	public void setDoOverwrite(boolean doOverwrite) {
		this.doOverwrite = doOverwrite;
	}
	
	

}