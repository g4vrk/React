package com.g4vrk.reactdataset.notification;

import java.nio.file.Path;

public interface RecordingNotifier {
    public void onFile(Path var1, int var2, int var3);

    public void onComplete(int var1, int var2);

    public void onError();

    public static RecordingNotifier none() {
        return new RecordingNotifier(){

            @Override
            public void onFile(Path path, int captured, int targetFiles) {
            }

            @Override
            public void onComplete(int captured, int targetFiles) {
            }

            @Override
            public void onError() {
            }
        };
    }
}
