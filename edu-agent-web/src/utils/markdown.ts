export function renderMarkdown(md: string): string {
  if (!md) return ''
  
  // 保护代码块
  const codeBlocks: string[] = []
  let html = md.replace(/```([^`]*)```/g, (_, code) => {
    codeBlocks.push('<pre><code>' + escapeHtml(code) + '</code></pre>')
    return '%%CODE' + (codeBlocks.length - 1) + '%%'
  })
  
  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  
  // 标题
  html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^# (.+)$/gm, '<h2>$1</h2>')
  
  // 粗体 / 斜体
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  
  // 列表项
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  
  // 段落
  html = html.replace(/\n\n/g, '</p><p>')
  html = html.replace(/\n/g, '<br>')
  
  // 包裹段落
  html = '<p>' + html + '</p>'
  
  // 包裹列表
  html = html.replace(/(<li>.*?<\/li>)/g, '<ul>$1</ul>')
  
  // 还原代码块
  for (let i = 0; i < codeBlocks.length; i++) {
    html = html.replace('%%CODE' + i + '%%', codeBlocks[i])
  }
  
  return html
}

function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}
