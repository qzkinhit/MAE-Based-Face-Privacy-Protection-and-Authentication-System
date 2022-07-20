# --------------------------------------------------------
# Based on BEiT, timm, DINO and DeiT code bases
# https://github.com/microsoft/unilm/tree/master/beit
# https://github.com/rwightman/pytorch-image-models/tree/master/timm
# https://github.com/facebookresearch/deit
# https://github.com/facebookresearch/dino
# --------------------------------------------------------'
# 随机遮罩生成
import random
import math
import numpy as np
import torch
import string


class RandomMaskingGenerator:
    def __init__(self, input_size, mask_ratio, pwd, appendant, checkpwd):
        if not isinstance(input_size, tuple):  # 如果输入不是数组
            input_size = (input_size,) * 2  # 变成二元数组

        self.height, self.width = input_size
        self.num_patches = self.height * self.width
        self.num_mask = int(mask_ratio * self.num_patches)
        self.pwd = pwd
        self.appendant = appendant
        self.checkpwd = checkpwd

    def __repr__(self):
        repr_str = "Maks: total patches {}, mask patches {}".format(
            self.num_patches, self.num_mask
        )
        return repr_str

    def setup_seed(self, seed):
        torch.manual_seed(seed)
        torch.cuda.manual_seed_all(seed)
        np.random.seed(seed)
        random.seed(seed)
        torch.backends.cudnn.deterministic = True

    def getSeed(self, pwd, appendant, checkpwd):
        secret = 0;
        if checkpwd:
            for i in pwd:
                secret = secret*255 + ord(i);
            # 0-255,减少碰撞
        else:
            for i in pwd:
                secret = secret*2333 + ord(i);
        seed=(secret+ appendant)%(2**32);#变换范围
        return seed;

    def __call__(self):
        seed = self.getSeed(self.pwd, self.appendant, self.checkpwd)
        print("seed是:", seed)
        self.setup_seed(seed)
        # print(self.num_patches)
        mask = np.hstack([
            np.zeros(self.num_patches - self.num_mask),
            np.ones(self.num_mask),
        ])
        np.random.shuffle(mask)  # 随机排序
        return mask  # [196]
