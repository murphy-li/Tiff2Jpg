import json

from PIL import Image
import numpy as np


def __image_compose__(size, images, data):

    """
    定义图像拼接函数
    :param size:
    :param images:
    :param data:
    :return:
    """
    w, h = size
    res = Image.new('RGB', (w, h))
    # 循环遍历，把每张图片按顺序粘贴到对应位置上
    x = 0
    y = 0
    index = 1
    list = []
    for image in images:
        res.paste(Image.fromarray(image), (x, y))
        h, w, c = image.shape
        dictionary = dict()
        dictionary['no'] = index
        dictionary['top'] = y
        dictionary['width'] = w
        dictionary['height'] = h
        y += h
        index += 1
        list.append(dictionary)
    data['pos'] = list
    return res


def __resolve_size__(images):
    """
    处理size
    :param images:
    :return:
    """
    width = 0
    height = 0
    for image in images:
        h, w, c = image.shape
        height += h
        width = width if width > w else w
    return width, height


def __read_tiff__(path):
    """
    读取tiff文件
    :param path:
    :return:
    """
    name, format = path.split('.')
    if format != 'tiff' and format != 'tif':
        raise ValueError("文件名格式不正确", path)
    img = Image.open(path)
    images = []
    for i in range(img.n_frames):
        img.seek(i)
        images.append(np.array(img))
    return images


def transfer_tiff_jpg(input_path, save_path):
    data = dict()
    images = __read_tiff__(input_path)
    w, h = __resolve_size__(images)
    data['spriteW'] = w
    data['spriteH'] = h
    res = __image_compose__((w, h), images, data)
    res.save(save_path)
    return json.dumps(data)


if __name__ == "__main__":
    # conda install libtiff=4.0.10
    print(transfer_tiff_jpg("C:/Users/xxx/Desktop/44.tiff", "1.jpg"))
