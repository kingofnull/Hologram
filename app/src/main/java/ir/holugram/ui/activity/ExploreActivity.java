package ir.holugram.ui.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import butterknife.BindView;
import ir.holugram.R;

public class ExploreActivity extends BaseDrawerActivity {

    @BindView(R.id.tlSearchExplorTabs)
    TabLayout tlSearchExplorTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.search_toolbar_explore);
        myToolbar.setTitle("");
        setSupportActionBar(myToolbar);

        setupTabs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView =
                (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setQueryHint("جستجو...");

        // Configure the search info and add any event listeners...
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // your text view here
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(ExploreActivity.this, query, Toast.LENGTH_SHORT).show();
                searchView.clearFocus();
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void setupTabs() {
        tlSearchExplorTabs.addTab(tlSearchExplorTabs.newTab().setIcon(R.drawable.ic_grid_on_white));
        tlSearchExplorTabs.addTab(tlSearchExplorTabs.newTab().setIcon(R.drawable.ic_list_white));
        tlSearchExplorTabs.addTab(tlSearchExplorTabs.newTab().setIcon(R.drawable.ic_place_white));
        tlSearchExplorTabs.addTab(tlSearchExplorTabs.newTab().setIcon(R.drawable.ic_label_white));
    }


}
