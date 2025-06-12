# RRO overlays naming conventions

## AOSP vs AOSP with Lineage additions vs Lineage-only

We add `.lineage` and increase priority only when we overlay LineageOS additions to AOSP components

## Name

`[Lineage]<component name>Overlay(Common|Device)`

- `android`: `FrameworkRes`
- `com.android.carrierconfig`: `CarrierConfig`
- `com.android.dialer`: `Dialer`
- `com.android.phone`: `Telephony`
- `com.android.providers.settings`: `SettingsProvider`
- `com.android.settings`: `Settings`
- `com.android.systemui`: `SystemUI`
- `com.android.wifi.resources:WifiCustomization`: `WifiResources`
- `lineageos.platform`: `LineageSDK`
- `org.lineageos.aperture`: `Aperture`

## Package name

`<target package>.overlay[.lineage].(common|device)`

## Priority

- AOSP common overlay: 100
- Lineage common overlay: 150
- AOSP device overlay: 200
- Lineage device overlay: 250
