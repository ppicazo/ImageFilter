package com.phonegap.plugins.imageFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;
import java.util.UUID;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

*change to app
//import com.yourapp.phonegap.R;

public class ImageFilter extends Plugin {

	private final String TAG = "ImageFilter";
	
	private final String RESIZED_FILENAME = "resized.png";
	private final String NO_MEDIA = ".nomedia";
	private final String DIRECTORY = "changetoappname";

	@Override
	public PluginResult execute(String action, JSONArray optionsArr, String callBackId) {
		
		Log.d(TAG, "PLUGIN execute called with action: " + action);

		try {

			//the first string in the array will always be the string URI to the image
			String inputString = optionsArr.getString(0);
			
			Log.d(TAG, "input string: " + inputString);

			if (inputString == null) {
				return new PluginResult(PluginResult.Status.ERROR, "Input uri is null");
			}

			if (action.equals("resize")) {

				String outputString = resize(inputString);

				if (outputString != null) {

					return new PluginResult(PluginResult.Status.OK, outputString);
				}
				else {
					return new PluginResult(PluginResult.Status.ERROR, "There was an error resizing the image");
				}
			}
			else if (action.equals("rotate")) {
				
				//the degree of rotation
				int direction = optionsArr.getInt(1);

				String outputString = rotate(inputString, direction);

				if (outputString != null) {
					return new PluginResult(PluginResult.Status.OK, outputString);
				}
				else {
					return new PluginResult(PluginResult.Status.ERROR, "There was an error resizing the image");
				}

			}

			else if (action.equals("filter")) {

				//the name of the filter and the name of the border
				String filter = optionsArr.getString(1);
				String border = optionsArr.getString(2);

				String outputString = renderImage(inputString, filter, border);

				if (outputString != null) {

					return new PluginResult(PluginResult.Status.OK, outputString);
				}
				else {
					return new PluginResult(PluginResult.Status.ERROR, "There was an error resizing the image");
				}
			}
			else if (action.equals("save")) {

				Boolean success = saveImageToGallery(inputString);

				if (success == true) {

					return new PluginResult(PluginResult.Status.OK, "Image saved to gallery");
				}
				else {
					return new PluginResult(PluginResult.Status.ERROR, "There was an error saving the image");
				}
			}

			else {
				return new PluginResult(PluginResult.Status.INVALID_ACTION, "unknown action");
			}

		}
		catch (JSONException e) {
			e.printStackTrace();
			return new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage());
		}
		catch (IOException e) {
			return new PluginResult(PluginResult.Status.IO_EXCEPTION, e.getMessage());
		}
	}

	private String resize(String inputString) throws IOException {

		InputStream is = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(inputString));
		Bitmap bmp = BitmapFactory.decodeStream(is);
		is.close();

		int width = bmp.getWidth();
		int height = bmp.getHeight();

		//hard coded dimensions
		int newWidth = 725;
		int newHeight = 725;

		int offsetX = 0;
		int offsetY = 0;

		if (width > height) {
			offsetX = (width - height) / 2;
		}
		else if (height > width) {
			offsetY = (height - width) / 2;
		}

		float scaleWidth = ((float) newWidth) / (width - (offsetX * 2));
		float scaleHeight = ((float) newHeight) / (height - (offsetY * 2));

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		bmp = Bitmap.createBitmap(bmp, offsetX, offsetY, width - (offsetX * 2), height - (offsetY * 2), matrix, true);

		String path = Environment.getExternalStorageDirectory().toString() + File.separator + DIRECTORY;

		File outputDir = new File(path);

		//create the directory and add .nomedia so gallery does not pick up the image
		if (!outputDir.exists()) {
			outputDir.mkdir();
			// add the no media file
			File media = new File(path + File.separator + NO_MEDIA);
			media.createNewFile();
		}

		File outputFile = new File(path + File.separator + RESIZED_FILENAME);

		FileOutputStream out = new FileOutputStream(outputFile);

		bmp.compress(Bitmap.CompressFormat.PNG, 100, out);

		bmp.recycle();

		return Uri.fromFile(outputFile).toString();

	}

	private String rotate(String inputString, int direction) throws IOException {

		InputStream is = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(inputString));
		Bitmap bmp = BitmapFactory.decodeStream(is);
		is.close();

		int width = bmp.getWidth();
		int height = bmp.getHeight();

		Matrix matrix = new Matrix();

		// degree rotation
		matrix.postRotate(direction);

		bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);

		String path = Environment.getExternalStorageDirectory().toString() + File.separator + DIRECTORY;

		File outputDir = new File(path);

		if (!outputDir.exists()) {
			outputDir.mkdir();
			File media = new File(path + File.separator + NO_MEDIA);
			media.createNewFile();

		}

		File outputFile = new File(path + File.separator + RESIZED_FILENAME);

		FileOutputStream out = new FileOutputStream(outputFile);
		bmp.compress(Bitmap.CompressFormat.PNG, 100, out);

		bmp.recycle();

		return Uri.fromFile(outputFile).toString();

	}
	
	private String renderImage(String inputString, String filterName, String borderName) throws IOException {

		InputStream is = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(inputString));
		Bitmap bmp = BitmapFactory.decodeStream(is);
		is.close();

		bmp = Bitmap.createBitmap(bmp);

		// need to make the bitmap mutable for canvas constructor
		bmp = convertToMutable(bmp);

		// create the canvas off the bitmap
		Canvas canvas = new Canvas(bmp);
		canvas.drawBitmap(bmp, 0, 0, new Paint());

		//if we have a filter name figure out which one
		if (!filterName.equalsIgnoreCase("none")) {

			Paint paint = new Paint();
			ColorMatrix cm = new ColorMatrix();

			if (filterName.equalsIgnoreCase("stark")) {

				Paint spaint = new Paint();
				ColorMatrix scm = new ColorMatrix();

				scm.setSaturation(0);
				final float m[] = scm.getArray();
				final float c = 1;
				scm.set(new float[] { m[0] * c, m[1] * c, m[2] * c, m[3] * c, m[4] * c + 15, m[5] * c, m[6] * c,
						m[7] * c, m[8] * c, m[9] * c + 8, m[10] * c, m[11] * c, m[12] * c, m[13] * c, m[14] * c + 10,
						m[15], m[16], m[17], m[18], m[19] });

				spaint.setColorFilter(new ColorMatrixColorFilter(scm));
				Matrix smatrix = new Matrix();
				canvas.drawBitmap(bmp, smatrix, spaint);

				cm.set(new float[] { 1, 0, 0, 0, -90, 0, 1, 0, 0, -90, 0, 0, 1, 0, -90, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("sunnyside")) {

				cm.set(new float[] { 1, 0, 0, 0, 10, 0, 1, 0, 0, 10, 0, 0, 1, 0, -60, 0, 0, 0, 1, 0 });
			}
			else if (filterName.equalsIgnoreCase("worn")) {

				cm.set(new float[] { 1, 0, 0, 0, -60, 0, 1, 0, 0, -60, 0, 0, 1, 0, -90, 0, 0, 0, 1, 0 });
			}
			else if (filterName.equalsIgnoreCase("grayscale")) {

				float[] matrix = new float[] { 0.3f, 0.59f, 0.11f, 0, 0, 0.3f, 0.59f, 0.11f, 0, 0, 0.3f, 0.59f, 0.11f,
						0, 0, 0, 0, 0, 1, 0, };

				cm.set(matrix);

			}
			else if (filterName.equalsIgnoreCase("cool")) {

				cm.set(new float[] { 1, 0, 0, 0, 10, 0, 1, 0, 0, 10, 0, 0, 1, 0, 60, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter0")) {

				cm.set(new float[] { 1, 0, 0, 0, 30, 0, 1, 0, 0, 10, 0, 0, 1, 0, 20, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter1")) {

				cm.set(new float[] { 1, 0, 0, 0, -33, 0, 1, 0, 0, -8, 0, 0, 1, 0, 56, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter2")) {

				cm.set(new float[] { 1, 0, 0, 0, -42, 0, 1, 0, 0, -5, 0, 0, 1, 0, -71, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter3")) {

				cm.set(new float[] { 1, 0, 0, 0, -68, 0, 1, 0, 0, -52, 0, 0, 1, 0, -15, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter4")) {

				cm.set(new float[] { 1, 0, 0, 0, -24, 0, 1, 0, 0, 48, 0, 0, 1, 0, 59, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter5")) {

				cm.set(new float[] { 1, 0, 0, 0, 83, 0, 1, 0, 0, 45, 0, 0, 1, 0, 8, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter6")) {

				cm.set(new float[] { 1, 0, 0, 0, 80, 0, 1, 0, 0, 65, 0, 0, 1, 0, 81, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter7")) {

				cm.set(new float[] { 1, 0, 0, 0, -44, 0, 1, 0, 0, 38, 0, 0, 1, 0, 46, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("filter8")) {

				cm.set(new float[] { 1, 0, 0, 0, 84, 0, 1, 0, 0, 63, 0, 0, 1, 0, 73, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("random")) {

				//pick an integer between -90 and 90 apply
				int min = -90;
				int max = 90;
				Random rand = new Random();

				int five = rand.nextInt(max - min + 1) + min;
				int ten = rand.nextInt(max - min + 1) + min;
				int fifteen = rand.nextInt(max - min + 1) + min;

				Log.d(TAG, "five " + five);
				Log.d(TAG, "ten " + ten);
				Log.d(TAG, "fifteen " + fifteen);

				cm.set(new float[] { 1, 0, 0, 0, five, 0, 1, 0, 0, ten, 0, 0, 1, 0, fifteen, 0, 0, 0, 1, 0 });

			}
			else if (filterName.equalsIgnoreCase("sepia")) {

				float[] sepMat = { 0.3930000066757202f, 0.7689999938011169f, 0.1889999955892563f, 0, 0,
						0.3490000069141388f, 0.6859999895095825f, 0.1679999977350235f, 0, 0, 0.2720000147819519f,
						0.5339999794960022f, 0.1309999972581863f, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1 };
				cm.set(sepMat);
			}

			paint.setColorFilter(new ColorMatrixColorFilter(cm));
			Matrix matrix = new Matrix();
			canvas.drawBitmap(bmp, matrix, paint);
		}

		// add a border
		if (!borderName.equalsIgnoreCase("none")) {

			Bitmap border = null;
			Bitmap scaledBorder = null;

			if (borderName.equalsIgnoreCase("black")) {

				border = BitmapFactory.decodeResource(cordova.getActivity().getResources(), R.drawable.black);
			}
			else if (borderName.equalsIgnoreCase("painter")) {

				border = BitmapFactory.decodeResource(cordova.getActivity().getResources(), R.drawable.painter);
			}
			else if (borderName.equalsIgnoreCase("vignette")) {

				border = BitmapFactory.decodeResource(cordova.getActivity().getResources(), R.drawable.vignette);
			}

			else if (borderName.equalsIgnoreCase("vintage")) {

				border = BitmapFactory.decodeResource(cordova.getActivity().getResources(), R.drawable.vintage);
			}
			else if (borderName.equalsIgnoreCase("white")) {

				border = BitmapFactory.decodeResource(cordova.getActivity().getResources(), R.drawable.white);
			}
			
			int width = bmp.getWidth();
			int height = bmp.getHeight();
			
			scaledBorder = Bitmap.createScaledBitmap(border,width,height, false);
			if (scaledBorder != null && border != null) {
				canvas.drawBitmap(scaledBorder, 0, 0, new Paint());
			}
		}

		String path = Environment.getExternalStorageDirectory().toString() + File.separator + DIRECTORY;

		File outputDir = new File(path);

		if (!outputDir.exists()) {
			outputDir.mkdir();
			File media = new File(path + File.separator + NO_MEDIA);
			media.createNewFile();

		}

		File outputFile = new File(path + File.separator + filterName + ".png");

		FileOutputStream out = new FileOutputStream(outputFile);
		bmp.compress(Bitmap.CompressFormat.PNG, 100, out);

		return Uri.fromFile(outputFile).toString();
	}

	private Boolean saveImageToGallery(String inputString) throws IOException {

		InputStream is = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(inputString));
		Bitmap bmp = BitmapFactory.decodeStream(is);
		is.close();

		String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
				+ File.separator + DIRECTORY;

		File outputDir = new File(path);

		if (!outputDir.exists()) {
			outputDir.mkdir();

		}

		File outputFile = new File(path + File.separator + UUID.randomUUID().toString() + ".png");

		FileOutputStream out = new FileOutputStream(outputFile);
		return bmp.compress(Bitmap.CompressFormat.PNG, 100, out);

		

	}

	private static Bitmap convertToMutable(Bitmap imgIn) {
		try {
			// this is the file going to use temporally to save the bytes.
			// This file will not be a image, it will store the raw image data.
			File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

			// Open an RandomAccessFile
			// Make sure you have added uses-permission
			// android:name="android.permission.WRITE_EXTERNAL_STORAGE"
			// into AndroidManifest.xml file
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			// get the width and height of the source bitmap.
			int width = imgIn.getWidth();
			int height = imgIn.getHeight();
			Config type = imgIn.getConfig();

			// Copy the byte to the file
			// Assume source bitmap loaded using options.inPreferredConfig =
			// Config.ARGB_8888;
			FileChannel channel = randomAccessFile.getChannel();
			MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
			imgIn.copyPixelsToBuffer(map);
			// recycle the source bitmap, this will be no longer used.
			imgIn.recycle();
			System.gc();// try to force the bytes from the imgIn to be released

			// Create a new bitmap to load the bitmap again. Probably the memory
			// will be available.
			imgIn = Bitmap.createBitmap(width, height, type);
			map.position(0);
			// load it back from temporary
			imgIn.copyPixelsFromBuffer(map);
			// close the temporary file and channel , then delete that also
			channel.close();
			randomAccessFile.close();

			// delete the temp file
			file.delete();

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return imgIn;
	}

}