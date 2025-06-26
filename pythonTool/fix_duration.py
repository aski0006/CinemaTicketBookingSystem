import re
import os
import shutil


def fix_sql_data(sql_content):
    """
    Fixes duration and release_date formats in the SQL content.
    - duration: '120分钟' -> 120
    - release_date: '2025' -> '2025-01-01'
    """
    lines = sql_content.split("\n")
    fixed_lines = []
    for line in lines:
        modified_line = line
        # Fix duration: '123分钟', -> 123,
        modified_line = re.sub(r"'\s*(\d+)\s*分钟\s*'", r"\1", modified_line)

        # Fix date: '2025', -> '2025-01-01',
        # This regex is safer as it assumes the year is the only thing on the line (plus whitespace and comma)
        if re.match(r"^\s*'(\d{4})',\s*$", modified_line.strip()):
            modified_line = re.sub(r"'(\d{4})'", r"'\1-01-01'", modified_line)

        fixed_lines.append(modified_line)
    return "\n".join(fixed_lines)


def main():
    # Use relative paths from the script's location
    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_path = os.path.join(script_dir, r"../src/main/resources/movies_info.sql")
    output_path = os.path.join(
        script_dir, r"../src/main/resources/movies_info_fixed.sql"
    )

    try:
        # Read original file
        with open(input_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Fix data formats
        fixed_content = fix_sql_data(content)

        # Write to new file
        with open(output_path, "w", encoding="utf-8") as f:
            f.write(fixed_content)

        print(f"处理完成！新文件已保存为: {output_path}")

        # Backup and replace original file
        backup_path = input_path + ".backup"
        # remove old backup if exists
        if os.path.exists(backup_path):
            os.remove(backup_path)
        shutil.move(input_path, backup_path)
        shutil.move(output_path, input_path)

        print(f"原文件已备份为: {backup_path}")
        print(f"新文件已替换原文件: {input_path}")

    except FileNotFoundError:
        print(f"错误: 找不到文件 {input_path}。请确认文件路径是否正确。")
    except Exception as e:
        print(f"处理过程中出错: {str(e)}")


if __name__ == "__main__":
    main()
