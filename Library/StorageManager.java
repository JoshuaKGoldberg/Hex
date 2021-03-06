package com.teamhex.cooler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.util.Log;

public class StorageManager {
	// Constructor
	// Required: String filename, Context context
	// The initial list of PaletteRecords is loaded using the index file
	public StorageManager() { Log.w("TeamHex", "A StorageManager is being created without a file name or context!"); }
	public StorageManager(Context _context) { this(_context, "RecordsIndex"); }
	public StorageManager(Context _context, String _filename) {
		_filename += ".txt";
		context = _context;
		fileIndexName = _filename;
		
		Log.i("TeamHex", "1. Creating StorageManager using file index " + _filename);

		// Records are stored in a hash table, by name
		records = new HashMap<String, PaletteRecord>();
		record_names = new ArrayList<String>();
		
		// Immediately attempt to load the record names from the file
		// Keep in mind these are viewable in the File Explorer
		// data > data > com.teamhex.cooler > files
		
		// 1. Check if the file exists, and make it if it doesn't
		// try {
			Log.i("TeamHex", "   Checking if file " + _filename + " exists...");
			File checker = new File(_filename);
			if(!checker.exists()) {
				/*
				Log.i("TeamHex", "      File " + _filename + " does not yet exist, attempting to create...");
				OutputStreamWriter osw = getFileWriter(_filename);
				osw.write("");
				osw.close();
				Log.i("TeamHex", "      File " + _filename + " successfully created.");
				*/
				Log.w("TeamHex", "      File " + _filename + " not found, but it might already exist! :(");
			}
			else {
				Log.i("TeamHex", "      File " + _filename + " successfully found.");
			}
		// }
		/*
		catch (IOException e) {
			Log.e("TeamHex", "   Could not create file " + _filename + ": " + e.toString());
		}
		*/
		Log.i("TeamHex", "   File checks on file " + _filename + " complete.");
		
		// 2. Get a reader to the index file
		Log.i("TeamHex", "2. Creating a reader to the index file");
		BufferedReader br;
		try {
			br = getFileReader(_filename);
		}
		catch (FileNotFoundException e1) {
			Log.i("TeamHex", e1.toString());
			Log.e("TeamHex", "Index file " + _filename + " could not be read: " + e1.toString());
			return;
		}
		
		
		// 3. Read the list of names from the index file
		try {
			Log.i("TeamHex", "3. Loading list of record names.");
			String buffer;
			while((buffer = br.readLine()) != null) {
				Log.i("TeamHex", "   Found record name: " + buffer);
				record_names.add(buffer);
			}
			Log.i("TeamHex", "   Finished finding record names.");
		}
		catch(IOException e1) {
			Log.e("TeamHex", "Error reading list of names from index file (" + _filename + "): " + e1.toString());
			return;
		}
		
		// 4. X11 name generation is lazily loaded in the X11Helper
		Log.i("TeamHex", "4. Making the X11Helper - not loading by default.");
		X11_names = new X11Helper();
		
		Log.i("TeamHex", "Finished making a StorageManager using file index " + _filename);
	}
	
	// Loads the file for the given record 
	public void RecordLoad(String name) {
		// Attempt to load the file into the PaletteRecord
		try {
			BufferedReader br = getFileReader(name + ".txt");
			records.put(name, new PaletteRecord(name, br));
			br.close();
		}
		catch (IOException e1) {
			Log.e("TeamHex", "Record file " + name + " could not be read: " + e1.toString());
			return;
		}
	}
	
	// Loads the next Num records
	public void RecordLoadNum(int num) {
		Log.i("TeamHex", "Attempting to load the next " + Integer.toString(num) + " record" + (num == 1 ? "" : "s") + ".");
		int i = num_loaded,
			max = Math.min(record_names.size(), num_loaded + num);
		while(i < max) {
			Log.i("TeamHex", "   Record load " + Integer.toString(i) + ": " + record_names.get(i));
			RecordLoad(record_names.get(i));
			++i;
		}
		num_loaded = max;
	}
	
	// Saves the file for the given record
	public void RecordSave(String name) {
		// Make sure that record name exists
		if(!records.containsKey(name)) {
			Log.w("TeamHex", "Attempting to save a record of name " + name + " that doesn't exist.");
			return;
		}
		RecordSave(records.get(name), name);
	}
	public void RecordSave(PaletteRecord record) { RecordSave(record, record.getName()); }
	public void RecordSave(PaletteRecord record, String name) {
		Log.i("TeamHex", "Saving record under name " + name);
		// Attempt to save the PaletteRecord into the file
		try {
			OutputStreamWriter osw = getFileWriter(name + ".txt");
			osw.write(record.getSaveString());
			osw.close();
		}
		catch (IOException e1) {
			Log.e("TeamHex", "Record file " + name + " could not be saved: " + e1.toString());
		}
	}
	

    /* 
     * Utility functions 
     */

    // getFileReader
    // Simply creates a new BufferedReader
    public BufferedReader getFileReader(String filename) throws FileNotFoundException {
    	FileInputStream fis = context.openFileInput(filename);
    	InputStreamReader isr = new InputStreamReader(fis);
    	BufferedReader br = new BufferedReader(isr);
    	return br;
    }
    
    // getFileWriter
    // Simply creates a new OutputStreamWriter
    public OutputStreamWriter getFileWriter(String filename) throws FileNotFoundException {
    	FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
    	OutputStreamWriter osw = new OutputStreamWriter(fos);
    	return osw;
    }
    
    // getColorName
    // Pipe to the X11Helper's getColorName
    public String getColorName(String hex) {
    	return X11Helper.getColorName(hex);
    }
    
    
	// Gets
	public Context getContext()      { return context; }
	public String getFileIndexName() { return fileIndexName; }
	
	// Sets
	public void setContext(Context context)            { this.context = context; }
	public void setFileIndexName(String fileIndexName) { this.fileIndexName = fileIndexName; }

	// Privates
	private Context context;					// Passed in from main(activity); used for file loading
	private String fileIndexName; 				// The storage file containing the names of the records  
	private int num_loaded; 					// How many records have been loaded so far
	private ArrayList<String> record_names; 	// The ordered list of record names
	private Map<String, PaletteRecord> records; // The stored records, keyed by name
	private X11Helper X11_names; 				// Used for color name analysis
}