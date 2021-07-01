package com.research.usage_stats;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.research.usage_stats.ui.MainActivity;

public class EnterDetails extends AppCompatActivity {
    EditText editText_rollNumber;
    Button button_submit;
    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_details);
        editText_rollNumber= findViewById(R.id.edt_rollNumber);
        button_submit= findViewById(R.id.btn_submit);
        button_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rollNum =editText_rollNumber.getText().toString();
                if(rollNum.length()<11){
                    editText_rollNumber.setError("Please Enter Full Roll Number");
                }else{
                    hideKeyboard(EnterDetails.this);
                    SharedPreferences preferences= getSharedPreferences(getString(R.string.roll_number),MODE_PRIVATE);
                    SharedPreferences.Editor editor= preferences.edit();
                    editor.putString(getString(R.string.roll_number),rollNum).apply();
                    startActivity(new Intent(EnterDetails.this, MainActivity.class));
                    finish();
                }
            }
        });

    }
    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
