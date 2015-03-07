package org.gnucash.android.ui.chart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.gnucash.android.R;
import org.gnucash.android.ui.passcode.PassLockActivity;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class ChartReportActivity extends PassLockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_chart_report);
        super.onCreate(savedInstanceState);

        findViewById(R.id.pie_chart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), PieChartActivity.class));
            }
        });
        findViewById(R.id.line_chart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(view.getContext(), LineChartActivity.class));
            }
        });
        findViewById(R.id.bar_chart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(view.getContext(), BarChartActivity.class));
            }
        });

    }
}
