package com.setuppicker;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;

/**
 * The hiscores' boss icons, for setups named after a boss, made small enough for a row of the list. Used from
 * the client thread only, as the game's sprites are.
 */
@Singleton
class BossIcons
{
	private final SpriteManager spriteManager;
	// by setup name, so each name is matched once
	private final Map<String, Optional<HiscoreSkill>> bosses = new HashMap<>();
	// by sprite id
	private final Map<Integer, BufferedImage> icons = new HashMap<>();

	@Inject
	BossIcons(SpriteManager spriteManager)
	{
		this.spriteManager = spriteManager;
	}

	/**
	 * The icon of the boss a setup of this name is for, at most width by height, or null when it isn't for a
	 * boss or the game hasn't loaded the icon yet.
	 */
	BufferedImage get(String setupName, int width, int height)
	{
		final HiscoreSkill boss = bosses.computeIfAbsent(setupName, name -> Optional.ofNullable(BossMatcher.match(name)))
			.orElse(null);
		if (boss == null)
		{
			return null;
		}
		final BufferedImage cached = icons.get(boss.getSpriteId());
		if (cached != null)
		{
			return cached;
		}
		final BufferedImage sprite = spriteManager.getSprite(boss.getSpriteId(), 0);
		if (sprite == null)
		{
			return null;
		}
		final BufferedImage icon = fit(sprite, width, height);
		icons.put(boss.getSpriteId(), icon);
		return icon;
	}

	/**
	 * The image, shrunk if needed to fit width by height, keeping its shape. Averaged rather than picking pixels,
	 * as the icons are shrunk by uneven amounts.
	 */
	static BufferedImage fit(BufferedImage image, int width, int height)
	{
		final double scale = Math.min(1, Math.min((double) width / image.getWidth(), (double) height / image.getHeight()));
		if (scale == 1)
		{
			return image;
		}
		final int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
		final int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
		final BufferedImage fitted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = fitted.createGraphics();
		g.drawImage(image.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING), 0, 0, null);
		g.dispose();
		return fitted;
	}
}
