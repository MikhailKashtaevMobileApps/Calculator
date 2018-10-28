package com.example.mike.calculator;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Calculator calculator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calculator = new Calculator();

        calculator.setDisplayView(findViewById(R.id.display));
        calculator.setResultView(findViewById(R.id.result));
    }

    public void click(View view) {
        TextView v = (TextView) view;
        calculator.addInput(v.getText().toString());
    }


    public void clear(View view) {
        calculator.clear();
    }
}
