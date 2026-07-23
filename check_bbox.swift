import Foundation
import CoreGraphics
import ImageIO

func getBoundingBox(imagePath: String) {
    guard let url = URL(string: "file://" + imagePath),
          let source = CGImageSourceCreateWithURL(url as CFURL, nil),
          let cgImage = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
        print("Could not load image at \(imagePath)")
        return
    }
    
    let width = cgImage.width
    let height = cgImage.height
    
    // Quick heuristic: we can just print the image size for now, 
    // actually extracting pixels in Swift takes a bit of code.
    print("\(imagePath): size=\(width)x\(height)")
}

let paths = [
    "composeApp/src/commonMain/composeResources/drawable/ic_control.png",
    "composeApp/src/commonMain/composeResources/drawable/ic_device_connection.png",
    "composeApp/src/commonMain/composeResources/drawable/ic_face.png",
    "composeApp/src/commonMain/composeResources/drawable/ic_remote_desktop.png"
]

for path in paths {
    let fullPath = FileManager.default.currentDirectoryPath + "/" + path
    getBoundingBox(imagePath: fullPath)
}
