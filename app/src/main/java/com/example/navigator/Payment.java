package com.example.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;


public class Payment extends AppCompatActivity implements View.OnClickListener {

    private ImageView debitImage;
    private ImageView eftImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        debitImage = (ImageView) findViewById(R.id.imageCredit);
        eftImage = (ImageView) findViewById(R.id.imageEFT);

        debitImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), Modal.class);
                view.getContext().startActivity(intent);}
        });
    }

    @Override
    public void onClick(View view) {

    }
}
