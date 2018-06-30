package me.amyhgu.flickster;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import me.amyhgu.flickster.models.Config;
import me.amyhgu.flickster.models.Movie;

import static me.amyhgu.flickster.MovieListActivity.API_BASE_URL;
import static me.amyhgu.flickster.MovieListActivity.API_KEY_PARAM;
import static me.amyhgu.flickster.MovieListActivity.TAG;

public class MovieDetailsActivity extends AppCompatActivity {

    Movie movie;
    Config config;
    Context context;
    AsyncHttpClient client;
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvOverview) TextView tvOverview;
    @BindView(R.id.rbVoteAverage) RatingBar rbVoteAverage;
    @BindView(R.id.ivBackdropImage) ImageView ivBackdropImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        // find view objects with ButterKnife
        ButterKnife.bind(this);

        client = new AsyncHttpClient();
        // unwrap the movie passed in via intent, using its simple name as a key
        movie = (Movie) Parcels.unwrap(getIntent().getParcelableExtra(Movie.class.getSimpleName()));
        config = (Config) Parcels.unwrap(getIntent().getParcelableExtra(Config.class.getSimpleName()));
        Log.d("MovieDetailsActivity", String.format("Showing details for '%s'", movie.getTitle()));

        tvTitle.setText(movie.getTitle());
        tvOverview.setText(movie.getOverview());
        context = getApplicationContext();
        int placeholderId = R.drawable.flicks_backdrop_placeholder;
        getVideo();

        Glide.with(context)
                .load(config.getImageUrl(config.getBackdropSize(), movie.getBackdropPath()))
                .apply(RequestOptions.placeholderOf(placeholderId)
                        .error(placeholderId)
                        .fitCenter()
                        .transform(new RoundedCornersTransformation(25, 0, RoundedCornersTransformation.CornerType.ALL))
                ).into(ivBackdropImage);

        // convert vote average to 0-5 scale by dividing by 2
        float voteAverage = movie.getVoteAverage().floatValue();
        rbVoteAverage.setRating(voteAverage = voteAverage > 0 ? voteAverage / 2.0f : voteAverage);
    }

    // get the video link from the API
    private void getVideo() {
        String movieId = String.valueOf(movie.getId());
        String url = String.format(API_BASE_URL + "/movie/%s/videos", movieId);
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key));
        // execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                ivBackdropImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String key = null;
                        try {
                            JSONArray results = response.getJSONArray("results");
                            key = results.getJSONObject(0).getString("key");
                        } catch(JSONException e) {
                            logError("Failed to get video id", e, true);
                        }
                        Intent intent = new Intent(context, MovieTrailerActivity.class);
                        intent.putExtra("videoId", key);
                        context.startActivity(intent);
                    }
                });
                Log.i(TAG, "Loaded video id");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from video endpoint", throwable, true);
            }
        });
    }

    // handle errors, log and alert user
    private void logError(String message, Throwable error, boolean alertUser) {
        // always log the error
        Log.e(TAG, message, error);
        // fail in non-silent way to alert user
        if (alertUser) {
            // display a long toast with error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
