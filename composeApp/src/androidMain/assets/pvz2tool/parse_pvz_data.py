#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PvZ2 数据文件解析器
将 PvZ2中文版代码(至v3.9.2).txt 转换为结构化 JSON 文件
"""

import json
import re
import os
from pathlib import Path

# 输入文件路径
INPUT_FILE = "/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/src/androidMain/assets/pvz2tool/PvZ2中文版代码(至v3.9.2).txt"
# 输出目录
OUTPUT_DIR = "/Users/macbookpro/AndroidStudioProjects/Pvz2Restart/composeApp/src/androidMain/assets/pvz2tool/pvz/"

def read_file():
    """读取原始数据文件"""
    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        return f.readlines()

def parse_plant_types(lines, end_line=None):
    """解析植物代码"""
    plants = {}
    i = 0
    if end_line is None:
        end_line = len(lines)
    
    # 先找到植物章节的开始
    while i < end_line:
        line = lines[i].strip()
        if '★植物代码' in line:
            i += 1
            break
        i += 1
    
    # 现在解析植物数据
    while i < end_line:
        line = lines[i].strip()
        
        # 遇到下一个章节标记，停止
        if line.startswith('★'):
            break
        
        # 跳过空行和标题行
        if not line or '植物映射值' in line or '多套装扮' in line:
            i += 1
            continue
        
        # 检测植物主行
        parts = line.split()
        if len(parts) >= 4:
            try:
                idx = parts[0]
                if idx.isdigit() or (len(idx) == 3 and idx.startswith('0')):
                    name_cn = parts[1]
                    plant_code = parts[2]
                    id_plant = parts[3]
                    
                    # 跳过挂件数据：name_cn 应该是中文名，不是数字
                    if name_cn.isdigit():
                        i += 1
                        continue
                    
                    id_shard = parts[4] if len(parts) > 4 else ""
                    id_avatar = parts[5] if len(parts) > 5 else ""
                    id_avatar_shard = parts[6] if len(parts) > 6 else ""
                    
                    order = int(idx) if idx.isdigit() else int(idx.lstrip('0'))
                    
                    plant = {
                        "id": int(id_plant) if id_plant.isdigit() else id_plant,
                        "code": plant_code,
                        "name": name_cn,
                        "order": order,
                        "shardId": int(id_shard) if id_shard.isdigit() else None,
                        "avatarId": int(id_avatar) if id_avatar.isdigit() else None,
                        "avatarShardId": int(id_avatar_shard) if id_avatar_shard.isdigit() else None,
                        "avatars": []
                    }
                    
                    i += 1
                    # 查找装扮行
                    while i < end_line:
                        avatar_line = lines[i].strip()
                        if not avatar_line or avatar_line.startswith('★'):
                            break
                        
                        next_parts = avatar_line.split()
                        if len(next_parts) >= 1 and next_parts[0].isdigit():
                            break
                        
                        avatar_parts = avatar_line.split()
                        if len(avatar_parts) >= 2:
                            avatar_name = avatar_parts[0]
                            avatar_id = avatar_parts[1] if len(avatar_parts) > 1 else ""
                            avatar_shard_id = avatar_parts[2] if len(avatar_parts) > 2 else ""
                            
                            plant["avatars"].append({
                                "name": avatar_name,
                                "id": int(avatar_id) if avatar_id.isdigit() else None,
                                "shardId": int(avatar_shard_id) if avatar_shard_id.isdigit() else None
                            })
                        i += 1
                    
                    plants[plant_code] = plant
                    continue
            except (ValueError, IndexError):
                pass
        
        i += 1
    
    return plants

def parse_zombie_types(lines, start_line, end_line):
    """解析僵尸代码"""
    zombies = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '僵尸映射值' in line:
            continue
        
        parts = line.split()
        if len(parts) >= 3:
            try:
                idx = parts[0]
                name = parts[1]
                code = parts[2]
                
                zombies[code] = {
                    "id": int(idx) if idx.isdigit() else idx,
                    "name": name,
                    "code": code
                }
            except (ValueError, IndexError):
                pass
    
    return zombies

def parse_game_feature(lines, start_line, end_line):
    """解析强化道具代码"""
    features = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '强化道具编号' in line:
            continue
        
        parts = line.split(None, 2)  # 最多分割2次
        if len(parts) >= 3:
            try:
                idx = parts[0]
                name = parts[1]
                code = parts[2]
                
                features[code] = {
                    "id": int(idx) if idx.isdigit() else idx,
                    "name": name,
                    "code": code
                }
            except (ValueError, IndexError):
                pass
    
    return features

def parse_world_name(lines, start_line, end_line):
    """解析世界代码"""
    worlds = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '世界编号' in line:
            continue
        
        parts = line.split(None, 2)
        if len(parts) >= 3:
            try:
                idx = parts[0]
                name = parts[1]
                code = parts[2]
                
                worlds[code] = {
                    "id": int(idx) if idx.isdigit() else idx,
                    "name": name,
                    "code": code
                }
            except (ValueError, IndexError):
                pass
    
    return worlds

def parse_level_modules(lines, start_line, end_line):
    """解析关卡模块代码"""
    modules = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '关卡模块名称' in line or line.startswith('背景'):
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            name = parts[0]
            code = parts[1]
            
            modules[code] = {
                "name": name,
                "code": code
            }
    
    return modules

def parse_grid_item_types(lines, start_line, end_line):
    """解析障碍物代码"""
    items = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line:
            continue
        
        # 跳过标题行
        if '障碍物代码' in line or '挂件代码' in line or '金手指' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            name = parts[0]
            code = parts[1].replace('(RTID)', '').replace('(TypeName)', '').strip()
            
            items[code] = {
                "name": name,
                "code": code
            }
    
    return items

def parse_projectile_types(lines, start_line, end_line):
    """解析子弹代码"""
    projectiles = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '子弹名称' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            code = parts[0]
            name = parts[1]
            
            projectiles[code] = {
                "name": name,
                "code": code
            }
    
    return projectiles

def parse_tool_packets(lines, start_line, end_line):
    """解析传送带卡槽代码"""
    packets = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '传送带卡槽名称' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            name = parts[0]
            code = parts[1]
            
            packets[code] = {
                "name": name,
                "code": code
            }
    
    return packets

def parse_property_sheets(lines, start_line, end_line):
    """解析属性/挂件代码"""
    props = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '挂件代码' in line or '挂件数字代码' in line:
            continue
        
        parts = line.split(None, 3)
        if len(parts) >= 4:
            try:
                id_item = parts[0]
                id_shard = parts[1]
                name = parts[2]
                code = parts[3]
                
                props[code] = {
                    "id": int(id_item) if id_item.isdigit() else id_item,
                    "shardId": int(id_shard) if id_shard.isdigit() else id_shard,
                    "name": name,
                    "code": code
                }
            except (ValueError, IndexError):
                pass
    
    return props

def parse_resources(lines, start_line, end_line):
    """解析资源代码"""
    resources = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '资源数字代码' in line:
            continue
        
        parts = line.split(None, 2)
        if len(parts) >= 3:
            try:
                id_res = parts[0]
                name = parts[1]
                code = parts[2]
                
                resources[code] = {
                    "id": int(id_res) if id_res.isdigit() else id_res,
                    "name": name,
                    "code": code
                }
            except (ValueError, IndexError):
                pass
    
    return resources

def parse_avatar_codes(lines, start_line, end_line):
    """解析头像代码"""
    avatars = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line:
            continue
        
        # 跳过标题
        if '头像' in line or '数字代码' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            try:
                id_avatar = parts[0]
                name = parts[1]
                
                avatars[id_avatar] = {
                    "id": int(id_avatar) if id_avatar.isdigit() else id_avatar,
                    "name": name
                }
            except (ValueError, IndexError):
                pass
    
    return avatars

def parse_plant_families(lines, start_line, end_line):
    """解析植物家族代码"""
    families = {}
    
    i = start_line
    while i < min(end_line, len(lines)):
        line = lines[i].strip()
        
        # 跳过属性行
        if '植物家族属性代码' in line or '属性名称' in line or '增益属性' in line:
            i += 1
            continue
        
        if '★' in line:
            break
        
        # 解析家族成员行
        parts = line.split()
        if len(parts) >= 2:
            try:
                family_id = parts[0]
                rest = ' '.join(parts[1:])
                
                # 分离家族名和成员
                match = re.match(r'^(.+?)\((.+)\)$', rest)
                if match:
                    family_name = match.group(1)
                    members_str = match.group(2)
                    members = [m.strip() for m in members_str.split('&')]
                else:
                    family_name = rest
                    members = []
                
                families[family_id] = {
                    "id": int(family_id) if family_id.isdigit() else family_id,
                    "name": family_name,
                    "members": members
                }
            except (ValueError, IndexError):
                pass
        
        i += 1
    
    return families

def parse_family_attributes(lines, start_line, end_line):
    """解析植物家族属性代码"""
    attrs = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line:
            continue
        
        if '属性名称' in line or '增益属性' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            name = parts[0]
            code = parts[1]
            
            attrs[code] = {
                "name": name,
                "code": code
            }
    
    return attrs

def parse_artifacts(lines, start_line, end_line):
    """解析神器代码"""
    artifacts = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '神器数字代码' in line:
            continue
        
        parts = line.split(None, 2)
        if len(parts) >= 3:
            try:
                id_art = parts[0]
                name = parts[1]
                code = parts[2]
                
                artifacts[code] = {
                    "id": int(id_art) if id_art.isdigit() else id_art,
                    "name": name,
                    "code": code
                }
            except (ValueError, IndexError):
                pass
    
    return artifacts

def parse_status_codes(lines, start_line, end_line):
    """解析状态代码"""
    statuses = {}
    current_section = None
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line:
            continue
        
        if line in ['僵尸', '植物', '障碍物']:
            current_section = line
            continue
        
        if '状态名称' in line or '状态字符' in line:
            continue
        
        statuses[line] = {
            "name": line,
            "section": current_section
        }
    
    return statuses

def parse_powerup_types(lines, start_line, end_line):
    """解析金手指代码"""
    powerups = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '金手指代码' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            name = parts[0]
            code = parts[1]
            
            powerups[code] = {
                "name": name,
                "code": code
            }
    
    return powerups

def parse_gacha_codes(lines, start_line, end_line):
    """解析藏品代码"""
    gacha = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '藏品代码' in line or '藏品数字代码' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            try:
                id_gacha = parts[0]
                name = parts[1]
                
                gacha[id_gacha] = {
                    "id": int(id_gacha) if id_gacha.isdigit() else id_gacha,
                    "name": name
                }
            except (ValueError, IndexError):
                pass
    
    return gacha

def parse_gene_codes(lines, start_line, end_line):
    """解析基因编辑代码"""
    genes = {}
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line or '基因编辑数字代码' in line or '基因编辑植物名称' in line:
            continue
        
        parts = line.split(None, 1)
        if len(parts) >= 2:
            try:
                id_gene = parts[0]
                name = parts[1]
                
                genes[id_gene] = {
                    "id": int(id_gene) if id_gene.isdigit() else id_gene,
                    "name": name
                }
            except (ValueError, IndexError):
                pass
    
    return genes

def parse_package_names(lines, start_line, end_line):
    """解析全渠道包名"""
    packages = []
    
    for i in range(start_line, min(end_line, len(lines))):
        line = lines[i].strip()
        if not line or '★' in line:
            continue
        
        if '杂项' in line or '全渠道包名' in line or '《' in line:
            continue
        
        packages.append(line)
    
    return packages

def main():
    """主函数"""
    print("读取文件...")
    lines = read_file()
    
    # 辅助函数：查找行范围
    def find_section(start_marker, end_marker=None):
        start = None
        end = None
        for i, line in enumerate(lines):
            if start_marker in line:
                start = i + 1
            elif end_marker and end_marker in line:
                end = i
                break
        return start, end
    
    # 解析各类型数据
    print("解析植物代码...")
    # 找到植物部分的结束位置（僵尸代码开始）
    plant_end = None
    for i, line in enumerate(lines):
        if '★僵尸代码' in line:
            plant_end = i
            break
    
    plants = parse_plant_types(lines, plant_end if plant_end else len(lines))
    
    print("解析僵尸代码...")
    z_start = None
    z_end = None
    for i, line in enumerate(lines):
        if '★僵尸代码' in line:
            z_start = i + 1
        elif '★强化道具' in line:
            z_end = i
            break
    if z_start:
        zombies = parse_zombie_types(lines, z_start, z_end if z_end else len(lines))
    
    print("解析强化道具...")
    f_start = None
    f_end = None
    for i, line in enumerate(lines):
        if '★强化道具代码' in line:
            f_start = i + 1
        elif '★世界代码' in line:
            f_end = i
            break
    if f_start:
        features = parse_game_feature(lines, f_start, f_end if f_end else len(lines))
    
    print("解析世界代码...")
    w_start = None
    w_end = None
    for i, line in enumerate(lines):
        if '★世界代码' in line:
            w_start = i + 1
        elif '★关卡模块' in line:
            w_end = i
            break
    if w_start:
        worlds = parse_world_name(lines, w_start, w_end if w_end else len(lines))
    
    print("解析关卡模块...")
    m_start = None
    m_end = None
    for i, line in enumerate(lines):
        if '★关卡模块代码' in line:
            m_start = i + 1
        elif '★障碍物代码' in line:
            m_end = i
            break
    if m_start:
        modules = parse_level_modules(lines, m_start, m_end if m_end else len(lines))
    
    print("解析障碍物...")
    g_start = None
    g_end = None
    for i, line in enumerate(lines):
        if '★障碍物代码' in line:
            g_start = i + 1
        elif '★金手指代码' in line:
            g_end = i
            break
    if g_start:
        grid_items = parse_grid_item_types(lines, g_start, g_end if g_end else len(lines))
    
    print("解析子弹代码...")
    p_start = None
    p_end = None
    for i, line in enumerate(lines):
        if '★子弹代码' in line:
            p_start = i + 1
        elif '★传送带卡槽' in line:
            p_end = i
            break
    if p_start:
        projectiles = parse_projectile_types(lines, p_start, p_end if p_end else len(lines))
    
    print("解析传送带卡槽...")
    t_start = None
    t_end = None
    for i, line in enumerate(lines):
        if '★传送带卡槽' in line:
            t_start = i + 1
        elif '★属性代码' in line:
            t_end = i
            break
    if t_start:
        tool_packets = parse_tool_packets(lines, t_start, t_end if t_end else len(lines))
    
    print("解析属性代码...")
    pr_start = None
    pr_end = None
    for i, line in enumerate(lines):
        if '★属性代码' in line:
            pr_start = i + 1
        elif '★资源代码' in line:
            pr_end = i
            break
    if pr_start:
        properties = parse_property_sheets(lines, pr_start, pr_end if pr_end else len(lines))
    
    print("解析资源代码...")
    r_start = None
    r_end = None
    for i, line in enumerate(lines):
        if '★资源代码' in line:
            r_start = i + 1
        elif '★头像代码' in line:
            r_end = i
            break
    if r_start:
        resources = parse_resources(lines, r_start, r_end if r_end else len(lines))
    
    print("解析头像代码...")
    av_start = None
    av_end = None
    for i, line in enumerate(lines):
        if '★头像代码' in line:
            av_start = i + 1
        elif '★排行榜背景代码' in line:
            av_end = i
            break
    if av_start:
        avatars = parse_avatar_codes(lines, av_start, av_end if av_end else len(lines))
    
    print("解析植物家族...")
    pf_start = None
    pf_end = None
    fa_start = None
    fa_end = None
    for i, line in enumerate(lines):
        if '★植物家族代码' in line:
            pf_start = i + 1
        elif '植物家族属性代码' in line:
            fa_start = i + 1
        elif '★神器代码' in line:
            fa_end = i
            pf_end = i
            break
    if pf_start:
        plant_families = parse_plant_families(lines, pf_start, pf_end if pf_end else len(lines))
    if fa_start:
        family_attrs = parse_family_attributes(lines, fa_start, fa_end if fa_end else len(lines))
    
    print("解析神器...")
    art_start = None
    art_end = None
    for i, line in enumerate(lines):
        if '★神器代码' in line:
            art_start = i + 1
        elif '★基因编辑代码' in line:
            art_end = i
            break
    if art_start:
        artifacts = parse_artifacts(lines, art_start, art_end if art_end else len(lines))
    
    print("解析状态代码...")
    s_start = None
    s_end = None
    for i, line in enumerate(lines):
        if '★状态代码' in line:
            s_start = i + 1
        elif '★全渠道包名' in line:
            s_end = i
            break
    if s_start:
        statuses = parse_status_codes(lines, s_start, s_end if s_end else len(lines))
    
    print("解析金手指...")
    pw_start = None
    pw_end = None
    for i, line in enumerate(lines):
        if '★金手指代码' in line:
            pw_start = i + 1
        elif '★子弹代码' in line:
            pw_end = i
            break
    if pw_start:
        powerups = parse_powerup_types(lines, pw_start, pw_end if pw_end else len(lines))
    
    print("解析藏品...")
    gc_start = None
    gc_end = None
    for i, line in enumerate(lines):
        if '★藏品代码' in line:
            gc_start = i + 1
        elif '★植物家族代码' in line:
            gc_end = i
            break
    if gc_start:
        gacha = parse_gacha_codes(lines, gc_start, gc_end if gc_end else len(lines))
    
    print("解析基因编辑...")
    gn_start = None
    gn_end = None
    for i, line in enumerate(lines):
        if '★基因编辑代码' in line:
            gn_start = i + 1
        elif '★状态代码' in line:
            gn_end = i
            break
    if gn_start:
        genes = parse_gene_codes(lines, gn_start, gn_end if gn_end else len(lines))
    
    print("解析包名...")
    pkg_start, _ = find_section('★全渠道包名')
    packages = parse_package_names(lines, pkg_start, len(lines))
    
    # 保存所有文件
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    print("保存文件...")
    
    with open(os.path.join(OUTPUT_DIR, "plants.json"), 'w', encoding='utf-8') as f:
        json.dump(plants, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "zombies.json"), 'w', encoding='utf-8') as f:
        json.dump(zombies, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "game_features.json"), 'w', encoding='utf-8') as f:
        json.dump(features, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "worlds.json"), 'w', encoding='utf-8') as f:
        json.dump(worlds, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "level_modules.json"), 'w', encoding='utf-8') as f:
        json.dump(modules, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "grid_items.json"), 'w', encoding='utf-8') as f:
        json.dump(grid_items, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "projectiles.json"), 'w', encoding='utf-8') as f:
        json.dump(projectiles, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "tool_packets.json"), 'w', encoding='utf-8') as f:
        json.dump(tool_packets, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "properties.json"), 'w', encoding='utf-8') as f:
        json.dump(properties, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "resources.json"), 'w', encoding='utf-8') as f:
        json.dump(resources, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "avatars.json"), 'w', encoding='utf-8') as f:
        json.dump(avatars, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "plant_families.json"), 'w', encoding='utf-8') as f:
        json.dump({
            "families": plant_families,
            "attributes": family_attrs
        }, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "artifacts.json"), 'w', encoding='utf-8') as f:
        json.dump(artifacts, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "statuses.json"), 'w', encoding='utf-8') as f:
        json.dump(statuses, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "powerups.json"), 'w', encoding='utf-8') as f:
        json.dump(powerups, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "gacha.json"), 'w', encoding='utf-8') as f:
        json.dump(gacha, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "genes.json"), 'w', encoding='utf-8') as f:
        json.dump(genes, f, ensure_ascii=False, indent=2)
    
    with open(os.path.join(OUTPUT_DIR, "packages.json"), 'w', encoding='utf-8') as f:
        json.dump(packages, f, ensure_ascii=False, indent=2)
    
    print("完成！")
    print(f"植物: {len(plants)} 个")
    print(f"僵尸: {len(zombies)} 个")
    print(f"强化道具: {len(features)} 个")
    print(f"世界: {len(worlds)} 个")
    print(f"关卡模块: {len(modules)} 个")
    print(f"障碍物: {len(grid_items)} 个")
    print(f"子弹: {len(projectiles)} 个")
    print(f"传送带卡槽: {len(tool_packets)} 个")
    print(f"属性/挂件: {len(properties)} 个")
    print(f"资源: {len(resources)} 个")
    print(f"头像: {len(avatars)} 个")
    print(f"植物家族: {len(plant_families)} 个")
    print(f"家族属性: {len(family_attrs)} 个")
    print(f"神器: {len(artifacts)} 个")
    print(f"状态: {len(statuses)} 个")
    print(f"金手指: {len(powerups)} 个")
    print(f"藏品: {len(gacha)} 个")
    print(f"基因编辑: {len(genes)} 个")
    print(f"包名: {len(packages)} 个")

if __name__ == "__main__":
    main()
