from pathlib import Path

def build_file_tree(directory, prefix="", is_root=True, ignore_hidden=True):
    directory = Path(directory)
    lines = []

    if is_root:
        lines.append(f"{directory.name}/")

    try:
        items = list(directory.iterdir())
    except PermissionError:
        lines.append(f"{prefix}└── [Permission Denied]")
        return lines

    if ignore_hidden:
        items = [item for item in items if not item.name.startswith(".")]

    # ترتيب: المجلدات أولًا ثم الملفات، أبجديًا
    items.sort(key=lambda x: (not x.is_dir(), x.name.lower()))

    for index, item in enumerate(items):
        is_last = index == len(items) - 1
        connector = "└── " if is_last else "├── "

        if item.is_dir():
            lines.append(f"{prefix}{connector}{item.name}/")
            extension = "    " if is_last else "│   "
            lines.extend(build_file_tree(item, prefix + extension, is_root=False, ignore_hidden=ignore_hidden))
        else:
            lines.append(f"{prefix}{connector}{item.name}")

    return lines


def save_file_tree_to_txt(directory, output_file="file_tree.txt", ignore_hidden=True):
    tree_lines = build_file_tree(directory, ignore_hidden=ignore_hidden)

    with open(output_file, "w", encoding="utf-8") as f:
        f.write("\n".join(tree_lines))

    print(f"✅ File tree saved to: {output_file}")


if __name__ == "__main__":
    directory_path = Path.cwd()  # أو ضع مسارًا مخصصًا هنا
    save_file_tree_to_txt(directory_path)