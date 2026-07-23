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
    
    guard let dataProvider = cgImage.dataProvider,
          let data = dataProvider.data,
          let ptr = CFDataGetBytePtr(data) else {
        return
    }
    
    let bytesPerPixel = cgImage.bitsPerPixel / 8
    let bytesPerRow = cgImage.bytesPerRow
    
    var minX = width
    var minY = height
    var maxX = 0
    var maxY = 0
    
    for y in 0..<height {
        for x in 0..<width {
            let offset = y * bytesPerRow + x * bytesPerPixel
            // Assuming RGBA or ARGB, check alpha channel
            // We just check if any byte is non-zero (or alpha specifically, but checking non-zero is a safe heuristic for transparent background)
            let alpha = ptr[offset + 3] // Assuming RGBA
            if alpha > 10 {
                if x < minX { minX = x }
                if x > maxX { maxX = x }
                if y < minY { minY = y }
                if y > maxY { maxY = y }
            }
        }
    }
    
    let bWidth = maxX - minX + 1
    let bHeight = maxY - minY + 1
    print("\(imagePath): size=\(width)x\(height), bbox=\(minX),\(minY) to \(maxX),\(maxY) -> innerSize=\(bWidth)x\(bHeight), ratio=\(Double(bWidth)/Double(width))")
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
