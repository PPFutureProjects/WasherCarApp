package androks.washerapp.Activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import net.cachapa.expandablelayout.ExpandableLayout;

import androks.washerapp.R;

public class WasherDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    CollapsingToolbarLayout collapsingToolbar;
    RecyclerView recyclerView;
    int mutedColor = R.attr.colorPrimary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_washer_details);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.anim_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle("Vianor");

        ImageView header = (ImageView) findViewById(R.id.header);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.vianor);
        header.setImageBitmap(bitmap);
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {
                mutedColor = palette.getMutedColor(R.color.primary_500);
                collapsingToolbar.setContentScrimColor(mutedColor);
                collapsingToolbar.setStatusBarScrimColor(R.color.black_trans80);
            }
        });

        findViewById(R.id.prices).setOnClickListener(this);
        findViewById(R.id.description).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        ExpandableLayout expandableLayout;
        switch (view.getId()){
            case R.id.prices:
                expandableLayout = (ExpandableLayout) findViewById(R.id.expandable_prices);
                expandableLayout.toggle(true);
                break;

            case R.id.description:
                expandableLayout = (ExpandableLayout) findViewById(R.id.expandable_description);
                expandableLayout.toggle(true);
                break;
        }
    }
}
