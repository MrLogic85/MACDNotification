package com.sleepyduck.macdnotification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by 23060767 on 11/14/13.
 */
public class ListItem extends FrameLayout {
    private EditText mSymbolText;
    private Button mValidateButton;
    private Button mRemvoeButton;
    public ListItem(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, null);
        addView(view);

        mSymbolText = (EditText) findViewById(R.id.editTextSymbol);
        mValidateButton = (Button) findViewById(R.id.buttonValidate);
        mRemvoeButton = (Button) findViewById(R.id.buttonRemove);

        mValidateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new CalculateMACD(getContext())
                        .execute(mSymbolText.getText().toString(), CalculateMACD.TOAST);
            }
        });

        mRemvoeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "Remove", Toast.LENGTH_LONG).show();
                ViewParent pv = ListItem.this.getParent();
                if (pv instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) pv;
                    layout.removeView(ListItem.this);
                }
            }
        });
    }
}
