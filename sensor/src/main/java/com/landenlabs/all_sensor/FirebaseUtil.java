/**
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landenlabs.all_sensor;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * https://code.tutsplus.com/tutorials/getting-started-with-cloud-firestore-for-android--cms-30382
 * https://firebase.google.com/products/firestore
 *
 * Utility class for initializing Firebase services and connecting them to the Firebase Emulator
 * Suite if necessary.
 */
public class FirebaseUtil {

    /** Use emulators only in debug builds **/
    private static final boolean sUseEmulators = false; // BuildConfig.DEBUG;

    private static FirebaseFirestore FIRESTORE;
    private static FirebaseAuth AUTH;
    private static AuthUI AUTH_UI;

    public static FirebaseFirestore getFirestore() {
        if (FIRESTORE == null) {
            FIRESTORE = FirebaseFirestore.getInstance();

            // Connect to the Cloud Firestore emulator when appropriate. The host '10.0.2.2' is a
            // special IP address to let the Android emulator connect to 'localhost'.
            if (sUseEmulators) {
                FIRESTORE.useEmulator("10.0.2.2", 8080);
            }
        }

        return FIRESTORE;
    }

    public static FirebaseAuth getAuth() {
        if (AUTH == null) {
            AUTH = FirebaseAuth.getInstance();

            // Connect to the Firebase Auth emulator when appropriate. The host '10.0.2.2' is a
            // special IP address to let the Android emulator connect to 'localhost'.
            if (sUseEmulators) {
                AUTH.useEmulator("10.0.2.2", 9099);
            }
        }

        return AUTH;
    }

    public static AuthUI getAuthUI() {
        if (AUTH_UI == null) {
            AUTH_UI = AuthUI.getInstance();

            // Connect to the Firebase Auth emulator when appropriate. The host '10.0.2.2' is a
            // special IP address to let the Android emulator connect to 'localhost'.
            if (sUseEmulators) {
                AUTH_UI.useEmulator("10.0.2.2", 9099);
            }
        }

        return AUTH_UI;
    }

    /*
       private FirebaseFirestore mFirestore;
       private Query mQuery;
       private void initDatabase() {
           FirebaseFirestore.setLoggingEnabled(true);
           mFirestore = FirebaseUtil.getFirestore();
       }

       DatabaseReference mDatabase;
       private void initDatabase() {
           mDatabase = FirebaseDatabase.getInstance().getReference();
           final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
           mDatabase.child("users").child(userId).addListenerForSingleValueEvent(
               new ValueEventListener() {
                   @Override
                   public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                       User user = dataSnapshot.getValue(User.class);

                       if (user == null) {
                           // User is null, error out
                           ALog.e.tagMsg(this, "User ", userId, " is unexpectedly null");
                           Toast.makeText(MainActivity.this, "Error: could not fetch user.", Toast.LENGTH_SHORT).show();
                       } else {
                           // Write new post
                           writeNewPost(userId, user.username, "testTitle", "testBody");
                       }
                   }

                   @Override
                   public void onCancelled(@NonNull DatabaseError databaseError) {
                       ALog.e.tagMsg(this, "getUser:onCancelled", databaseError.toException());
                   }
               });
       }

       private void writeNewPost(String userId, String username, String title, String body) {
           String key = mDatabase.child("start").push().getKey();
           Post post = new Post(userId, username, title, body);
           Map<String, Object> postValues = post.toMap();
           Map<String, Object> childUpdates = new HashMap<>();
           childUpdates.put("/posts/" + key, postValues);
           childUpdates.put("/user-posts/" + userId + "/" + key, postValues);
           Task<Void> dbUpd =  mDatabase.updateChildren(childUpdates);
           ALog.i.tagMsg(this, "GPS no power request=", StrUtils.toString(dbUpd));
       }

       private static class User {
           public String username;
           public String email;

           public User() {
               // Default constructor required for calls to DataSnapshot.getValue(User.class)
           }

           public User(String username, String email) {
               this.username = username;
               this.email = email;
           }
       }

       private static class Post {

           public String uid;
           public String author;
           public String title;
           public String body;
           public int starCount = 0;
           public Map<String, Boolean> stars = new HashMap<>();

           public Post() {
               // Default constructor required for calls to DataSnapshot.getValue(Post.class)
           }

           public Post(String uid, String author, String title, String body) {
               this.uid = uid;
               this.author = author;
               this.title = title;
               this.body = body;
           }

           @Exclude
           public Map<String, Object> toMap() {
               HashMap<String, Object> result = new HashMap<>();
               result.put("uid", uid);
               result.put("author", author);
               result.put("title", title);
               result.put("body", body);
               result.put("starCount", starCount);
               result.put("stars", stars);

               return result;
           }
       }
   */
}
