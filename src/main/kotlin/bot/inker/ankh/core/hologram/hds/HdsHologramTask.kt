package bot.inker.ankh.core.hologram.hds

import bot.inker.ankh.core.api.hologram.HologramContent
import bot.inker.ankh.core.api.hologram.HologramTask
import bot.inker.ankh.core.common.dsl.ensurePrimaryThread
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI
import me.filoghost.holographicdisplays.api.hologram.Hologram
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

class HdsHologramTask(
  private val hologram: Hologram,
) : HologramTask {
  override fun updateContent(content: HologramContent) {
    content as HdsHologramContent
    ensurePrimaryThread()
    content.applyToLines(hologram.lines)
  }

  override fun delete() {
    ensurePrimaryThread()
    hologram.delete()
  }

  class Builder(
    private val hdApi: HolographicDisplaysAPI,
  ) : HologramTask.Builder {
    private var location: Location? = null
    private var content: HdsHologramContent? = null

    override fun getThis() = this
    override fun content() = InnerContentBuilder(this)
    override fun content(content: HologramContent) = apply {
      this.content = content as HdsHologramContent
    }

    override fun location(location: Location) = apply {
      this.location = location
    }

    override fun build(): HologramTask {
      ensurePrimaryThread()
      val hologram = hdApi.createHologram(requireNotNull(location))
      val content = content ?: HdsHologramContent(emptyList())
      content.applyToLines(hologram.lines)
      return HdsHologramTask(hologram)
    }
  }

  class InnerContentBuilder(
    private val parent: Builder,
  ) : HologramTask.InnerContentBuilder {
    private val delegateBuilder = HdsHologramContent.Builder()
    override fun getThis() = this
    override fun build(): Builder {
      parent.content(delegateBuilder.build())
      return parent
    }

    override fun appendContent(content: String) = apply {
      delegateBuilder.appendContent(content)
    }

    override fun appendItem(item: ItemStack) = apply {
      delegateBuilder.appendItem(item)
    }
  }
}