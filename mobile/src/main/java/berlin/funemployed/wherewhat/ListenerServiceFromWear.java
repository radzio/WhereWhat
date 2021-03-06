package berlin.funemployed.wherewhat;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

import berlin.funemployed.wherewhat.common.Constants;
import info.metadude.java.library.overpass.ApiModule;
import info.metadude.java.library.overpass.models.OverpassResponse;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class ListenerServiceFromWear extends WearableListenerService {

    private GoogleApiClient googleApiClient;

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        if (googleApiClient == null || !googleApiClient.isConnected()) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            processMessageEvent(messageEvent);
                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    })
                    .build();
            googleApiClient.connect();
        } else {
            processMessageEvent(messageEvent);
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient!=null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    private void processMessageEvent(final MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        if (!path.equals(Constants.OVERPASS_REQUEST_FEATURES_PATH)) {
            Log.e(getClass().getName(), "Message event with unknown path: " + path);
            return;
        }

        final String requestString = new String(messageEvent.getData());

        ApiModule.provideOverpassService().getOverpassResponse(requestString).enqueue(new Callback<OverpassResponse>() {
            @Override
            public void onResponse(Response<OverpassResponse> response, Retrofit retrofit) {

                Moshi moshi = new Moshi.Builder().build();
                final JsonAdapter<List> adapter = moshi.adapter(List.class);
                final String jsonResponse = adapter.toJson(response.body().elements);
                Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), Constants.OVERPASS_RESPONSE_SUCCESS_PATH, jsonResponse.getBytes());
            }

            @Override
            public void onFailure(Throwable t) {
                Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), Constants.OVERPASS_RESPONSE_FAILURE_PATH, null);
            }
        });
    }
}
