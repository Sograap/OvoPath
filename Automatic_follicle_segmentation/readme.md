# Scripts to run automatic semantic segmentation for follicle detection

> [!WARNING]  
> This works for **QuPath 0.6** or above

### 1.Install qupath-extension-djl

To run the U-Nets inside **QuPath**, **OvoPath** uses the PyTorch engine within DeepJavaLearning extension for QuPath.

Fortunatelly, the **QuPath** already has it installed by defauld but you need to install the PyTorch engine

You need to go to `Extensions >Deep Java Library > Manage DJL engines`

There you will ifnd the PyTorch  (default) and need to press into **Check/Download** 

You can check this [here](https://qupath.readthedocs.io/en/latest/docs/deep/djl.html) for doubts


## 2. Download U-Net weights and tutorials

- The U-Net model weights and tutorials are available [here](https://drive.google.com/drive/folders/1PLWm6D0ISTOdkImtSeQu3klWU0VggQwi?usp=sharing).
- The tutorials cover all **OvoPath** features and will be updated over time.

#### Download & place the model(s)

1. Download the folder `Follicle_HE_unet_effnet` and unzip it.
- **Windows tip:** Sometimes unzipping creates a duplicated folder level (a folder inside a folder with the same name). If that happens, move the inner folder up one level so the files are where you expect them.
2. Create a directory for your models, e.g. on the Desktop: `OvoPath_unet_weights/`
3. Put each model folder inside that directory, for example:
```
OvoPath_unet_weights/
├─ Follicle_HE_unet_effnet/

```
#### Point OvoPath to your models

- **Scripting workflow:** Use the **full path** to the `.pt` file. Example: `C:/Users/Usuario/Desktop/OvoPath_unet_weights/Follicle_HE_unet_effnet/Follicle_HE_unet_effnet.pt`

#### Model folder contents (example)
Inside each model folder you should find:
- `HE_unet_effnetb4.pt` — **required** PyTorch traced model  
- `synset.txt` — **required** at runtime  

You’re ready to run **CPU inference**.
