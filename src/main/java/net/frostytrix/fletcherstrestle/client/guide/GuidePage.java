package net.frostytrix.fletcherstrestle.client.guide;

import java.util.List;

/** A single page of a sub-chapter: an ordered list of elements. */
public record GuidePage(List<GuideElement> elements) {

    public static GuidePage of(GuideElement... elements) {
        return new GuidePage(List.of(elements));
    }
}
