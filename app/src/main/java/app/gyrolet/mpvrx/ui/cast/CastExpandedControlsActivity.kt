package app.gyrolet.mpvrx.ui.cast

import android.view.Menu
import app.gyrolet.mpvrx.R
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity

class CastExpandedControlsActivity : ExpandedControllerActivity() {
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    menuInflater.inflate(R.menu.cast_expanded_controls, menu)
    CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
    return true
  }
}
