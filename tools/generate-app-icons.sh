#!/usr/bin/env bash
#
# Regenerates the example app's raster launcher icons from logo.svg.
#
#   ./tools/generate-app-icons.sh
#
# Covers the two places a vector will not do: Android's legacy pre-adaptive mipmaps and the iOS
# app icon. The Android adaptive icon (API 26+) is hand-maintained vector XML under
# example-app/src/androidMain/res/drawable/ and is NOT touched here - if the mark changes, update
# the pathData there to match logo.svg as well.
#
# Needs rsvg-convert, ImageMagick and cwebp:
#   brew install librsvg imagemagick webp

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
logo="$repo_root/logo.svg"
android_res="$repo_root/example-app/src/androidMain/res"
ios_appicon="$repo_root/example-app-ios/KiteUI Example App/Assets.xcassets/AppIcon.appiconset"

# Matches drawable/ic_launcher_background.xml. Gold on white loses the half-opacity chevron once
# the icon is launcher-sized, so the mark gets a navy field of its own.
background="#16203A"

# The drawn mark leaves a margin inside logo.svg's 1000-unit box, so rendering the file at N pixels
# only paints 0.8768*N of ink. Framing below is expressed as the fraction of the tile the *ink*
# should span, which is what the eye actually reads.
ink_ratio=0.8768

for tool in rsvg-convert magick cwebp; do
    command -v "$tool" >/dev/null || { echo "error: $tool not found (brew install librsvg imagemagick webp)" >&2; exit 1; }
done

# render_tile <tile_px> <ink_fraction> <out_png> [round]
# Draws the mark centered on an opaque background tile, optionally masked to a circle.
render_tile() {
    local tile=$1 ink_fraction=$2 out=$3 shape=${4:-square}
    local mark
    mark=$(python3 -c "print(round($tile * $ink_fraction / $ink_ratio))")

    local scratch
    scratch=$(mktemp -d)
    trap 'rm -rf "$scratch"' RETURN

    rsvg-convert -w "$mark" -h "$mark" "$logo" -o "$scratch/mark.png"
    magick -size "${tile}x${tile}" "xc:$background" \
        "$scratch/mark.png" -gravity center -composite \
        "$scratch/tile.png"

    # -depth 8 throughout: ImageMagick would otherwise promote to 16-bit, which App Store
    # validation flags on the iOS icon.
    if [ "$shape" = round ]; then
        magick -size "${tile}x${tile}" xc:none \
            -fill white -draw "circle $((tile / 2)),$((tile / 2)) $((tile / 2)),0" \
            "$scratch/mask.png"
        magick "$scratch/tile.png" "$scratch/mask.png" \
            -alpha off -compose CopyOpacity -composite -depth 8 "$out"
    else
        magick "$scratch/tile.png" -alpha off -depth 8 "$out"
    fi
}

echo "Android legacy mipmaps (pre-API-26 launchers, which apply no mask of their own)"
# 0.76 rather than the adaptive icon's 60/72: the round variant is masked to the tile's inscribed
# circle, and at 0.76 the mark's circumscribed circle clears it with room to spare.
for density in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
    name=${density%%:*}
    size=${density##*:}
    dir="$android_res/mipmap-$name"
    mkdir -p "$dir"

    render_tile "$size" 0.76 "$dir/ic_launcher.png" square
    render_tile "$size" 0.76 "$dir/ic_launcher_round.png" round

    for variant in ic_launcher ic_launcher_round; do
        cwebp -quiet -lossless "$dir/$variant.png" -o "$dir/$variant.webp"
        rm "$dir/$variant.png"
    done
    echo "  mipmap-$name  ${size}x${size}"
done

echo "iOS app icon (opaque, as the App Store requires; the system applies the corner mask)"
mkdir -p "$ios_appicon"
# Roomier framing than Android: iOS draws icons at a larger corner radius and Apple's own marks
# sit well inside the tile.
render_tile 1024 0.62 "$ios_appicon/icon-1024.png" square
echo "  icon-1024.png  1024x1024"

echo "Done."
