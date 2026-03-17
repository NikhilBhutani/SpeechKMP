.PHONY: gen open clean watch

# Regenerate the Xcode project from project.yml
gen:
	xcodegen generate

# Regenerate and open in Xcode
open: gen
	open DeviceAILabs.xcodeproj

# Nuke derived data for this project (fixes weird Xcode caching issues)
clean:
	rm -rf ~/Library/Developer/Xcode/DerivedData/DeviceAILabs-*

# Watch Sources/ and project.yml — auto-regenerates on any change.
# Run in a separate terminal tab alongside Xcode: make watch
# Xcode will show a "reload" banner when the project file changes.
watch:
	fswatch -o Sources/ project.yml | xargs -n1 -I{} sh -c 'xcodegen generate --quiet && echo "✓ project refreshed"'
