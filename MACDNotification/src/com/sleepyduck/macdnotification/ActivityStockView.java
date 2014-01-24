package com.sleepyduck.macdnotification;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sleepyduck.macdnotification.data.StockDataList;
import com.sleepyduck.macdnotification.data.Symbol;

public class ActivityStockView extends Activity {

	private Symbol mSymbol;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stock_view);

		Intent intent = getIntent();
		if (intent.hasExtra("symbol")) {
			mSymbol = (Symbol) intent.getSerializableExtra("symbol");

			TextView name = (TextView) findViewById(R.id.textViewName);
			TextView value = (TextView) findViewById(R.id.textViewValue);
			TextView ruleNo1Text = (TextView) findViewById(R.id.ruleNo1Text);
			ImageView ruleNo1SMA = (ImageView) findViewById(R.id.ruleNo1SMAIcon);
			ImageView ruleNo1Stochastic = (ImageView) findViewById(R.id.ruleNo1StochasticIcon);
			ImageView ruleNo1MACD = (ImageView) findViewById(R.id.ruleNo1MACDIcon);

			if (mSymbol.getDisplayName().length() > 0) {
				name.setText(mSymbol.getDisplayName() + " (" + mSymbol.getName() + ")");
			} else {
				name.setText(mSymbol.getName());
			}
            if (mSymbol.hasStockData()) {
                StockDataList data = mSymbol.getStockData();
                value.setText("" + data.get(data.size()-1).Close);

				ruleNo1SMA.setImageResource(mSymbol.isRuleNo1SMALessThanValue() ? R.drawable.ic_green : R.drawable.ic_red);
				ruleNo1MACD.setImageResource(mSymbol.isRuleNo1HistogramPositive() ? R.drawable.ic_green : R.drawable.ic_red);
				ruleNo1Stochastic.setImageResource(mSymbol.isRuleNo1StochasticPositive() ? R.drawable.ic_green : R.drawable.ic_red);

				if (mSymbol.getRuleNo1Valuation() != null) {
					ruleNo1Text.setText("Rule #1 (" + (int) (data.get(data.size()-1).Close / mSymbol.getRuleNo1Valuation() * 100.0f) + "%):");
				} else {
					ruleNo1Text.setText("Rule #1:");
				}
			} else {
				ruleNo1Text.setText("Rule #1:");
			}
		}
	}

	public void onYahooClicked(View view) {
        Uri uri = Uri.parse("http://finance.yahoo.com/q/ta?s=" + mSymbol.getName() + "&t=1y&l=on&z=l&q=l&p=e18%2Cb&a=m26-12-9%2Css&c=");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
	}

}
