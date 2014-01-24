package com.sleepyduck.macdnotification;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepyduck.macdnotification.data.StockDataList;
import com.sleepyduck.macdnotification.data.Symbol;

public class RuleNo1Indicators extends LinearLayout {

	private TextView mText;
	private ImageView mSMA;
	private ImageView mStochastic;
	private ImageView mMACD;
	private ImageView mStochasticOpt;
	private ImageView mValue;
	private Symbol mSymbol;

	public RuleNo1Indicators(Context context) {
		super(context);
		init(context);
	}

	public RuleNo1Indicators(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RuleNo1Indicators(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		LayoutInflater  inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.view_rule_no1_indicators, this);

		mText = (TextView) (findViewById(R.id.ruleNo1Text));
		mSMA = (ImageView) (findViewById(R.id.ruleNo1SMAIcon));
		mStochastic = (ImageView) (findViewById(R.id.ruleNo1StochasticIcon));
		mMACD = (ImageView) (findViewById(R.id.ruleNo1MACDIcon));
		mStochasticOpt = (ImageView) (findViewById(R.id.ruleNo1StochasticOptionalIcon));
		mValue = (ImageView) (findViewById(R.id.ruleNo1ValueationIcon));

		mText.setText("Rule #1:");
		mSMA.setImageResource(R.drawable.ic_blue);
		mStochastic.setImageResource(R.drawable.ic_blue);
		mMACD.setImageResource(R.drawable.ic_blue);
		mStochasticOpt.setImageResource(R.drawable.ic_blue);
		mValue.setImageResource(R.drawable.ic_blue);
	}

	public void setSymbol(Symbol symbol) {
		mSymbol = symbol;
		invalidateData();
	}

	public void invalidateData() {
		if (mSymbol != null && mSymbol.hasStockData()) {
			StockDataList data = mSymbol.getStockData();
			mSMA.setImageResource(mSymbol.isRuleNo1SMALessThanValue()
					? R.drawable.ic_green
							: R.drawable.ic_red);
			mStochastic.setImageResource(mSymbol.isRuleNo1StochasticPositive()
					? R.drawable.ic_green
							: R.drawable.ic_red);
			mMACD.setImageResource(mSymbol.isRuleNo1HistogramPositive()
					? R.drawable.ic_green
							: R.drawable.ic_red);

			if (mSymbol.isRuleNo1StochasticAbove80()) {
				mStochasticOpt.setImageResource(R.drawable.ic_green);
			} else if (mSymbol.isRuleNo1StochasticBelow20()) {
				mStochasticOpt.setImageResource(R.drawable.ic_red);
			} else {
				mStochasticOpt.setImageResource(R.drawable.ic_blue);
			}

			if (mSymbol.hasRuleNo1Valuation()) {
				mValue.setVisibility(VISIBLE);
				if (mSymbol.isValueBelowValuation50()) {
					mValue.setImageResource(R.drawable.ic_green);
				} else if (mSymbol.isValueAboveValuation()) {
					mValue.setImageResource(R.drawable.ic_red);
				} else {
					mValue.setImageResource(R.drawable.ic_blue);
				}
			} else {
				mValue.setVisibility(GONE);
			}

			if (mSymbol.getRuleNo1Valuation() != null) {
				mText.setText("Rule #1 ("
						+ toPercent(mSymbol.getRuleNo1Valuation(),
								data.get(data.size()-1).Close) + "%):");
			} else {
				mText.setText("Rule #1:");
			}
		} else {
			mText.setText("Rule #1:");
		}
	}

	private int toPercent(float total, float part) {
		return (int) (part / total * 100.0f);
	}
}
