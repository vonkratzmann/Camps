package au.com.mysites.camps.util;

public class ToDoNotes {

    //todo Created 20/6/201 Take api key out of android manifest
    //remove repository from github, once above fixed add back to repository

    //todo Created 20/6/201 update database backup and restore so they handle ratings
    //todo Created 20/6/201 convert latitude/Longitude so stored in the database
    //  in the native Geographical point
    //todo Created 20/6/201 in backup and restore break transactions into manageable sizes
    //todo Created 20/6/201 check while addsite handles restarts correctly, may loose data eg
        // if (savedInstanceState == null) {
        //restore variables image paths etc
        //fix so firebase listeners are unregistered on activity lifecycle changes, then restored
    //  when activity restarts
    // Created 20/6/2018 Done 26/8/2017 move storage of photos away from database into cloud
    //  and just have thumbnail in the database

    // LOW PRIORITY
    //todo Created 20/6/201 add directions to google maps

    /* Bugs */
    /*
    1. Created 20/6/201 on main activity thumbnail arbitrarily displays icons

    2. Created 20/6/201 Fixed 26/08/2018 When a photo is taken in add new site,
        the photo path in firebase storage is not stored in the database

    3. Created 20/6/201 On restore of database app hangs

    4. Created 20/6/20 1n Detail site, comments recycler view have to set fixed size
        otherwise doesn't scroll

    */
}
