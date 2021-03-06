package com.example.galzaid.movies.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.galzaid.movies.Actor;
import com.example.galzaid.movies.Constants;
import com.example.galzaid.movies.Movie;
import com.example.galzaid.movies.R;
import com.example.galzaid.movies.adapters.MoviesAdapter;
import com.example.galzaid.movies.database.DBHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DrawerLayout.DrawerListener {

    //UI Objects
    private DrawerLayout mDrawerLayout;
    private RecyclerView moviesRv;
    private Toolbar toolbar;
    private NavigationView navigationView;

    //Objects
    private MoviesAdapter moviesAdapter;
    private ArrayList<Movie> movies;
    private ArrayList<Movie> searchResults;
    private ArrayList<Movie> inTheatersMovies;
    private GridLayoutManager gridLayoutManager;

    //Variables
    private int pageNumber = 1;
    private int inTheaterPageNumber = 1;
    private boolean isLoading;
    private boolean finishedLoadingInCinemas;
    private String movieId = "";
    private enum MoviesDisplayState {popular, inTheaters , favorites}
    private MoviesDisplayState moviesDisplayState;
    //Database
    private DBHelper database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        moviesRv = findViewById(R.id.movie_recycler);
        toolbar = findViewById(R.id.toolbar2);
        navigationView = findViewById(R.id.nav_view);
        // toolbar setup
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(Constants.popularTitle);
        }

        //set the actions bar settings
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        }
        movies = new ArrayList<>();
        inTheatersMovies = new ArrayList<>();
        searchResults = new ArrayList<>();
        // Init Recycler!!!
        moviesAdapter = new MoviesAdapter(movies, this);
        moviesRv.setAdapter(moviesAdapter);
        gridLayoutManager = new GridLayoutManager(this, 2);
        moviesRv.setNestedScrollingEnabled(false);
        moviesRv.setLayoutManager(gridLayoutManager);
        getMovieDataRequest();

        database = new DBHelper(this);
        mDrawerLayout.addDrawerListener(this);

        moviesDisplayState = MoviesDisplayState.popular;

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Log.i("Touched", item.getItemId() + "");
                switch (item.getItemId()) {
                    case R.id.favorites:
                        mDrawerLayout.closeDrawers();
                        showFavorites();
                         moviesDisplayState = MoviesDisplayState.favorites;
                        getSupportActionBar().setTitle(Constants.favoriteTitle); // TODO make it change the title.
                        break;
                    case R.id.Popular:
                        getSupportActionBar().setTitle(Constants.popularTitle); // TODO make it change the title.
                        showPopular();
                        moviesDisplayState = MoviesDisplayState.popular;
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.in_theaters:
                        getSupportActionBar().setTitle(Constants.inTheatersTitle);
                        if(inTheatersMovies.isEmpty()) inTheaterPageNumber = 1;
                        moviesDisplayState = MoviesDisplayState.inTheaters;
                        showInTheaters();
                        getNowPlaying();
                        mDrawerLayout.closeDrawers();
                }
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void renderResult(JsonObject movieData, String logoPath , ArrayList<Actor> actorArrayList , JsonArray actorJsonArr) {
        Movie movie;
        movie = Movie.fromJson(movieData, logoPath); // has been done just to check if the movie exists and has a name
        if (!movie.getMovieName().equals("") || movie.getMovieName() != null) {
            movie.setMovieSecondUrl(logoPath);
            movie.setActorArrayList(actorArrayList);
            movie.setActorJsonArrStr(actorJsonArr.toString());
            movies.add(movie);
            moviesAdapter.notifyItemChanged(movies.indexOf(movie));
        }
    }

    private void renderSearchedResult(JsonObject movieData) {
        Movie movie;
        String logoPath = movieData.getAsJsonObject().get("backdrop_path").toString();
        movie = Movie.fromJsonSearched(movieData, logoPath); // has been done just to check if the movie exists and has a name
        if (!movie.getMovieName().equals("") || movie.getMovieName() != null) {
            movie.setMovieSecondUrl(logoPath);
            movie.setActorArrayList(createActorArr(movieData.getAsJsonObject().get("credits").getAsJsonObject()
                    .get("cast").getAsJsonArray()));
            movie.setActorJsonArrStr(movieData.getAsJsonObject().get("credits").getAsJsonObject()
                    .get("cast").getAsJsonArray().toString());
            searchResults.add(movie);
            showSearched();
        }
    }

    public void getMovieDataRequest() {
        Ion.with(this)
                .load("https://api.themoviedb.org/3/movie/popular?api_key=" + Constants.API_KEY + Constants.page + pageNumber)
                .asJsonObject() // result comes as json obj
                .setCallback(new FutureCallback<JsonObject>() { // does the request in the background, the function called when request is over (onCompleted)
                    @Override
                    public void onCompleted(Exception e, final JsonObject result) {
                        if (e != null) {
                            e.printStackTrace(); // if there is an error e. TODO Handle exception
                            Toast.makeText(MainActivity.this, "Error with getting data", Toast.LENGTH_SHORT).show();
                            isLoading = false;
                        } else {
                            Log.i("TESTER", "" + (result == null));
                            if (result.getAsJsonObject() != null && result.getAsJsonObject().get("results") != null) {
                                for (int i = 0; i < result.getAsJsonObject().get("results").getAsJsonArray().size(); i++) {
                                    movieId = result.getAsJsonObject().getAsJsonObject().get("results").getAsJsonArray().get(i).getAsJsonObject().get("id").toString(); // TODO checck if needs a change!
                                    getMovieDataRequestFull(movieId);
                                }
                            }
                        }
                        isLoading = false;

                    }

                });

        moviesRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading) {
                        isLoading = true;
                        if(moviesDisplayState == MoviesDisplayState.popular) {
                            pageNumber = pageNumber + 1;
                            getMovieDataRequest();
                        }
                        else if (moviesDisplayState == MoviesDisplayState.inTheaters) {
                            inTheaterPageNumber = inTheaterPageNumber + 1;
                            getNowPlaying();
                        }
                    }
                }
            }
        });
    }

    public void getMovieDataRequestFull(String movieId) {
        Ion.with(this)
                .load("https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + Constants.API_KEY + "&append_to_response=credits")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace(); // if there is an error e. TODO Handle exception
                            Toast.makeText(MainActivity.this, "Error with getting data", Toast.LENGTH_SHORT).show();
                        } else {
                            if (result != null) {
                                String logoPath;
                                JsonArray actorsArrJson;
                                ArrayList<Actor> actorArrayList;
                                if (result.getAsJsonObject().get("backdrop_path") != null) {
                                    logoPath = result.getAsJsonObject().get("backdrop_path").toString();
                                    actorsArrJson = result.getAsJsonObject().get("credits").getAsJsonObject()
                                            .get("cast").getAsJsonArray();
                                    actorArrayList = createActorArr(actorsArrJson);
                                    renderResult(result, logoPath , actorArrayList , actorsArrJson);
                                }


                            }
                        }
                    }
                });
    }

    public void getMovieDataRequestFullInCinemas(String movieId) {
        Ion.with(this)
                .load("https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + Constants.API_KEY + "&append_to_response=credits")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace(); // if there is an error e. TODO Handle exception
                            Toast.makeText(MainActivity.this, "Error with getting data", Toast.LENGTH_SHORT).show();
                            Log.i("results", e.getMessage() + "");
                        } else {
                            if (result != null) {
                                String logoPath;
                                JsonArray actorsArrJson;
                                ArrayList<Actor> actorArrayList;
                                if (result.getAsJsonObject().get("backdrop_path") != null) {
                                    logoPath = result.getAsJsonObject().get("backdrop_path").toString();
                                    actorsArrJson = result.getAsJsonObject().get("credits").getAsJsonObject()
                                            .get("cast").getAsJsonArray();
                                    actorArrayList = createActorArr(actorsArrJson);
                                    renderInCinemasResult(result, logoPath , actorArrayList , actorsArrJson);
                                }


                            }
                        }
                    }
                });
    }


    public void renderInCinemasResult(JsonObject movieData, String logoPath , ArrayList<Actor> actorArrayList , JsonArray actorJsonArr) {
        Movie movie;
        movie = Movie.fromJson(movieData, logoPath); // has been done just to check if the movie exists and has a name
        //TODO add movie actors
        if (!movie.getMovieName().equals("") || movie.getMovieName() != null) {
            movie.setMovieSecondUrl(logoPath);
            movie.setActorArrayList(actorArrayList);
            movie.setActorJsonArrStr(actorJsonArr.toString());
            inTheatersMovies.add(movie);
            moviesAdapter.notifyItemChanged(movies.indexOf(movie));
        }
    }

    public void getAdditionalMovieData(int movieId) {
        Ion.with(this)
                .load("https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + Constants.API_KEY  + "&append_to_response=credits")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace(); // if there is an error e. TODO Handle exception
                            Toast.makeText(MainActivity.this, "Error with getting data", Toast.LENGTH_SHORT).show();
                            Log.i("results", e.getMessage() + "");
                        } else {
                            if (result != null) {
                                renderSearchedResult(result);
                            }
                        }
                    }
                });
    }

    public void getMovieSearchRequest(String searchTitle) {
        searchTitle = searchTitle.replaceAll(" ", Constants.urlSpace); // in the browser spaces are swapped with this value.
        Ion.with(this)
                .load("http://api.themoviedb.org/3/search/movie?api_key=" + Constants.API_KEY + "&query=" + searchTitle)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace(); // if there is an error e. TODO Handle exception
                            Toast.makeText(MainActivity.this, "Error with getting data", Toast.LENGTH_SHORT).show();
                            Log.i("results", e.getMessage());
                        } else {
                            int length = result.get("results").getAsJsonArray().size();
                            for (int i = 0; i < length; i++) {
                                int movieId = result.get("results").getAsJsonArray().get(i).getAsJsonObject().get("id").getAsInt();
                                getAdditionalMovieData(movieId);
                            }
                        }
                    }
                });
    }

    public void getNowPlaying() {
        Ion.with(this)
                .load("https://api.themoviedb.org/3/movie/now_playing?api_key=" + Constants.API_KEY + "&language=en-US&page=" + inTheaterPageNumber)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            e.printStackTrace(); // if there is an error e. TODO Handle exception
                            Toast.makeText(MainActivity.this, "Error with getting data", Toast.LENGTH_SHORT).show();
                            Log.i("results", e.getMessage());
                            isLoading = false;
                        } else if (result != null) {
                            if (result.getAsJsonObject() != null && result.getAsJsonObject().get("results") != null) {
                                for (int i = 0; i < result.getAsJsonObject().get("results").getAsJsonArray().size(); i++) {
                                    movieId = result.getAsJsonObject().getAsJsonObject().get("results").getAsJsonArray().get(i).getAsJsonObject().get("id").toString(); // TODO checck if needs a change!
                                    if(i + 1 == result.getAsJsonObject().get("results").getAsJsonArray().size()) finishedLoadingInCinemas = true;
                                    getMovieDataRequestFullInCinemas(movieId);
                                }
                            }
                        }
                        isLoading = false;
                    }

                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        final SearchView searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.setQuery("", false);
                searchView.setIconified(true); // Closes the searchView.
                toolbar.setTitle(query);
                if (searchResults != null) searchResults.clear();
                getMovieSearchRequest(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }

        });

        return true;
    }

    public void showFavorites() {
        ArrayList<Movie> favorites = database.getAllFavorites();
        moviesAdapter = new MoviesAdapter(favorites, this );
        moviesRv.setAdapter(moviesAdapter);
    }

    public void showPopular() {
        moviesAdapter = new MoviesAdapter(movies, this );
        moviesAdapter.notifyDataSetChanged();
        moviesRv.setAdapter(moviesAdapter);
    }
    public void showInTheaters() {
        moviesAdapter = new MoviesAdapter(inTheatersMovies, this);
        moviesAdapter.notifyDataSetChanged();
        moviesRv.setAdapter(moviesAdapter);
    }

    public void showSearched() {
        moviesAdapter = new MoviesAdapter(searchResults, this );
        moviesRv.setAdapter(moviesAdapter);
    }

    @Override
    public void onBackPressed() {
        if (toolbar != null) {
            if (moviesDisplayState != MoviesDisplayState.popular) {
                showPopular();
                getSupportActionBar().setTitle(Constants.popularTitle);
            } else super.onBackPressed();
        }
    }
    public ArrayList<Actor> createActorArr(JsonArray actorJsonArr) {
        ArrayList<Actor> actorArrayList = new ArrayList<>();
        for(int i = 0; i < actorJsonArr.size(); i++) {
            actorArrayList.add(Actor.createActorFromJson(actorJsonArr.get(i).getAsJsonObject()));
        }
        return actorArrayList;
    }

}

