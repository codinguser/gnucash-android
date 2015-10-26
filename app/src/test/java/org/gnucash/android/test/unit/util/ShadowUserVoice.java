package org.gnucash.android.test.unit.util;

import android.content.Context;

import com.uservoice.uservoicesdk.Config;
import com.uservoice.uservoicesdk.UserVoice;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Shadow class for uservoice during testing
 */
@Implements(UserVoice.class)
public class ShadowUserVoice {

    @Implementation
    public static void init(Config config, Context context){
        //do nothing
    }
}
