package code.name.monkey.retromusic.ui.fragments.player.simple;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.retro.musicplayer.backend.model.Song;
import com.retro.musicplayer.backend.util.LyricUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import code.name.monkey.retromusic.R;
import code.name.monkey.retromusic.helper.MusicPlayerRemote;
import code.name.monkey.retromusic.ui.fragments.base.AbsPlayerFragment;
import code.name.monkey.retromusic.ui.fragments.player.PlayerAlbumCoverFragment;
import code.name.monkey.retromusic.util.MusicUtil;
import code.name.monkey.retromusic.util.PreferenceUtil;
import code.name.monkey.retromusic.util.ToolbarColorizeHelper;
import code.name.monkey.retromusic.util.Util;

/**
 * @author Hemanth S (h4h13).
 */

public class SimplePlayerFragment extends AbsPlayerFragment implements PlayerAlbumCoverFragment.Callbacks {

    @BindView(R.id.gradient_background)
    View mView;
    @BindView(R.id.player_toolbar)
    Toolbar mToolbar;
    private Unbinder mUnbinder;
    private SimplePlaybackControlsFragment mSimplePlaybackControlsFragment;
    private int mLastColor;

    private AsyncTask updateIsFavoriteTask;
    private AsyncTask updateLyricsAsyncTask;

    @Override
    public void onDestroyView() {
        if (updateLyricsAsyncTask != null && !updateLyricsAsyncTask.isCancelled()) {
            updateLyricsAsyncTask.cancel(true);
        }
        if (updateIsFavoriteTask != null && !updateIsFavoriteTask.isCancelled()) {
            updateIsFavoriteTask.cancel(true);
        }
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simple_player, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        /*Hide status bar view for !full screen mode*/
        if (PreferenceUtil.getInstance(getContext()).getFullScreenMode()) {
            view.findViewById(R.id.status_bar).setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpSubFragments();
        setUpPlayerToolbar();
    }

    private void setUpSubFragments() {
        PlayerAlbumCoverFragment playerAlbumCoverFragment = (PlayerAlbumCoverFragment) getChildFragmentManager().findFragmentById(R.id.player_album_cover_fragment);
        playerAlbumCoverFragment.setCallbacks(this);
        mSimplePlaybackControlsFragment = (SimplePlaybackControlsFragment) getChildFragmentManager().findFragmentById(R.id.playback_controls_fragment);

    }

    @Override
    public int getPaletteColor() {
        return mLastColor;
    }

    @Override
    public void onShow() {
        mSimplePlaybackControlsFragment.show();
    }

    @Override
    public void onHide() {
        mSimplePlaybackControlsFragment.hide();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onColorChanged(int color) {
        getCallbacks().onPaletteColorChanged();
        mLastColor = color;
        mSimplePlaybackControlsFragment.setDark(color);

        ToolbarColorizeHelper.colorizeToolbar(mToolbar,
                ATHUtil.resolveColor(getContext(), R.attr.iconColor), getActivity());
    }

    @Override
    public void onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.getCurrentSong());
    }

    @Override
    protected void toggleFavorite(Song song) {
        super.toggleFavorite(song);
        if (song.id == MusicPlayerRemote.getCurrentSong().id) {
            updateIsFavorite();
        }
    }

    private void setUpPlayerToolbar() {
        mToolbar.inflateMenu(R.menu.menu_player);
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        mToolbar.setOnMenuItemClickListener(this);
    }


    @SuppressLint("StaticFieldLeak")
    private void updateIsFavorite() {
        if (updateIsFavoriteTask != null) updateIsFavoriteTask.cancel(false);
        updateIsFavoriteTask = new AsyncTask<Song, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Song... params) {
                Activity activity = getActivity();
                if (activity != null) {
                    return MusicUtil.isFavorite(getActivity(), params[0]);
                } else {
                    cancel(false);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Boolean isFavorite) {
                Activity activity = getActivity();
                if (activity != null) {
                    int res = isFavorite ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_border_white_24dp;
                    int color = ATHUtil.resolveColor(activity, R.attr.iconColor, Color.TRANSPARENT);
                    Drawable drawable = Util.getTintedVectorDrawable(activity, res, color);
                    mToolbar.getMenu().findItem(R.id.action_toggle_favorite)
                            .setIcon(drawable)
                            .setTitle(isFavorite ? getString(R.string.action_remove_from_favorites) : getString(R.string.action_add_to_favorites));
                }
            }
        }.execute(MusicPlayerRemote.getCurrentSong());
    }

    @SuppressLint("StaticFieldLeak")
    private void updateLyrics() {
        if (updateLyricsAsyncTask != null) updateLyricsAsyncTask.cancel(false);
        final Song song = MusicPlayerRemote.getCurrentSong();
        updateLyricsAsyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mToolbar.getMenu().removeItem(R.id.action_show_lyrics);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                return LyricUtil.isLrcFileExist(song.title, song.artistName);

            }

            @Override
            protected void onPostExecute(Boolean l) {
                if (l) {
                    Activity activity = getActivity();
                    if (mToolbar != null && activity != null)
                        if (mToolbar.getMenu().findItem(R.id.action_show_lyrics) == null) {
                            int color = ATHUtil.resolveColor(activity, R.attr.iconColor, Color.TRANSPARENT);
                            Drawable drawable = Util.getTintedVectorDrawable(activity, R.drawable.ic_comment_text_outline_white_24dp, color);
                            mToolbar.getMenu()
                                    .add(Menu.NONE, R.id.action_show_lyrics, Menu.NONE, R.string.action_show_lyrics)
                                    .setIcon(drawable)
                                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        }
                } else {
                    if (mToolbar != null) {
                        mToolbar.getMenu().removeItem(R.id.action_show_lyrics);
                    }
                }

            }
        }.execute();
    }

}
