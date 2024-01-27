package com.azhar.spks.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.azhar.spks.R;
import com.azhar.spks.database.DatabaseHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HasilDiagnosaActivity extends AppCompatActivity {
    private Button btnSend;
    private EditText edtMessage;
    private RecyclerView rvMessage;



    SQLiteDatabase sqLiteDatabase;
    Toolbar toolbar;
    DatabaseHelper databaseHelper;
    TextView tvGejala, tvNamaPenyakit, tvPenanganan;
    ImageView imageview6666;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hasil_diagnosa);


        imageview6666 = findViewById(R.id.imageView6666);
        imageview6666.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HasilDiagnosaActivity.this, Login.class));
            }
        });

        setStatusBar();

        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        if (databaseHelper.openDatabase())
            sqLiteDatabase = databaseHelper.getReadableDatabase();


        tvGejala = findViewById(R.id.tvGejala);
        tvNamaPenyakit = findViewById(R.id.tvNamaPenyakit);
        tvPenanganan = findViewById(R.id.tvPenanganan);



        String str_hasil = getIntent().getStringExtra("HASIL");
        String[] gejala_terpilih = new String[0];
        if (str_hasil != null) {
            gejala_terpilih = str_hasil.split("#");
        }

        double cf_gabungan;
        double cf;
        HashMap<String, Double> mapHasil = new HashMap<>();

        String query_penyakit = "SELECT kode_penyakit FROM penyakit order by kode_penyakit";
        Cursor cursor_penyakit = sqLiteDatabase.rawQuery(query_penyakit, null);

        while (cursor_penyakit.moveToNext()) {
            cf_gabungan = (double) 0;
            int i = 0;
            String query_rule = "SELECT nilai_cf, kode_gejala FROM rule where kode_penyakit = '" + cursor_penyakit.getString(0) + "'";
            Cursor cursor_rule = sqLiteDatabase.rawQuery(query_rule, null);
            while (cursor_rule.moveToNext()) {
                cf = cursor_rule.getDouble(0);
                for (String s_gejala_terpilih : gejala_terpilih) {
                    String query_gejala = "SELECT kode_gejala FROM gejala where nama_gejala = '" + s_gejala_terpilih + "'";
                    Cursor cursor_gejala = sqLiteDatabase.rawQuery(query_gejala, null);
                    cursor_gejala.moveToFirst();
                    if (cursor_rule.getString(1).equals(cursor_gejala.getString(0))) {
                        if (i > 1) {
                            cf_gabungan = cf + (cf_gabungan * (1 - cf));
                        } else if (i == 1) {
                            cf_gabungan = cf_gabungan + (cf * (1 - cf_gabungan));
                        } else {
                            cf_gabungan = cf;
                        }
                        i++;
                    }
                    cursor_gejala.close();
                }
            }
            cursor_rule.close();
            mapHasil.put(cursor_penyakit.getString(0), cf_gabungan * 100);
        }
        cursor_penyakit.close();

        StringBuffer output_gejala_terpilih = new StringBuffer();
        int no = 1;
        for (String s_gejala_terpilih : gejala_terpilih) {
            output_gejala_terpilih.append(no++)
                    .append(". ")
                    .append(s_gejala_terpilih)
                    .append("\n");
        }

        tvGejala.setText(output_gejala_terpilih);

        Map<String, Double> sortedHasil = sortByValue(mapHasil);

        Map.Entry<String, Double> entry = sortedHasil.entrySet().iterator().next();
        String kode_penyakit = entry.getKey();
        double hasil_cf = entry.getValue();
        int persentase = (int) hasil_cf;

        String query_penyakit_hasil = "SELECT nama_penyakit, penanganan FROM penyakit where kode_penyakit='" + kode_penyakit + "'";
        Cursor cursor_hasil = sqLiteDatabase.rawQuery(query_penyakit_hasil, null);
        cursor_hasil.moveToFirst();

        tvNamaPenyakit.setText(cursor_hasil.getString(0) + " " + persentase + "%");

        tvPenanganan.setText(cursor_hasil.getString(1));

        cursor_hasil.close();

    }
    public static HashMap<String, Double> sortByValue(HashMap<String, Double> hm) {
        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(hm.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private void setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (on) {
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
        }
        window.setAttributes(layoutParams);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void openExternalLink(View view) {
        // Gantilink_dengan_URL_yang_ingin_dibuka
        String url = "https://youtu.be/eBeLBFf_hps?si=3KDSlbYuxNa9mA4d";

        // Membuat Intent untuk membuka browser
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        // Memulai aktivitas browser
        startActivity(intent);

    }


            }
