package com.njnu.kai.feisms;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class TabContactsActivity extends ListActivity {
	private static final String PREFIX = "TabContactsActivity";
	private Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_contact);
		int groupId = getIntent().getIntExtra(FeiSMSConst.KEY_GROUP_ID, 0);

//		Cursor cursor = getContentResolver().query(Contacts.CONTENT_URI, new String[] { Contacts._ID, Contacts.DISPLAY_NAME }, null, null,
//				Contacts.DISPLAY_NAME + " asc");
//		startManagingCursor(cursor);
//
//		ListAdapter listAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
//				new String[] { Contacts.DISPLAY_NAME }, new int[] { android.R.id.text1 });
//		setListAdapter(listAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(100, 100, 100, "Get Contacts");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getGroupId() == 100) {
			getContacts();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPostResume() {
		super.onPostResume();

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				View currentFocus = getListView();
				IBinder windowToken = currentFocus != null ? currentFocus.getWindowToken() : null;
				Log.i(PREFIX, "after onPostResume focusView=" + currentFocus + " token=" + windowToken);
				if (windowToken != null) {
					((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(windowToken,
							InputMethodManager.HIDE_NOT_ALWAYS);
				}
			}
		}, 0);
	}

	private ContactsData getContacts() {

		String[] personProjection = new String[] { ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.Contacts.HAS_PHONE_NUMBER };
		Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, personProjection, null, null,
				ContactsContract.Contacts.SORT_KEY_PRIMARY + " asc");

		ContactsData cData = new ContactsData(cur.getCount());
		int phoneNo = 0;
		if (cur.moveToFirst()) {
			do {

				long contactId = cur.getLong(0);
				String disPlayName = cur.getString(1);
				int phoneCount = cur.getInt(2);
				if (phoneCount > 0) {
					Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
					ContactsData.ContactsInfo cInfo = null;
					if (phones.moveToFirst()) {
						do {
							String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							if (SMSUtils.isChinesePhoneNumber(phoneNumber)) {
								if (cInfo == null) {
									cInfo = new ContactsData.ContactsInfo(disPlayName, phones.getCount());
								}
								cInfo.appendPhoneNumber(phoneNumber);
								++phoneNo;
							}
						} while (phones.moveToNext());
					}
					phones.close();
					if (cInfo != null) {
						cData.appendContactsInfo(cInfo);
					}
				}

			} while (cur.moveToNext());

		}
		cur.close();
		Log.i(PREFIX, "total phoneNumber is: " + phoneNo + " total person is:" + cData.getCount());
		return cData;
	}

}
