import React, { useEffect, useState } from 'react'
import { usePlugin } from '../Context'
import { lang } from '../../languages'
import toast from '../toast'

import Box from '@mui/material/Box'
import Toolbar from '@mui/material/Toolbar'
import Container from '@mui/material/Container'
import Card from '@mui/material/Card'
import CardHeader from '@mui/material/CardHeader'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import CircularProgress from '@mui/material/CircularProgress'

type Status = {
  enabled: boolean
  configured: boolean
  model: string
  baseUrl: string
}

type AiResponse = {
  ok: boolean
  answer?: string
  error?: string
}

const AiAssistant: React.FC = () => {
  const plugin = usePlugin()
  const [status, setStatus] = useState<Status | null>(null)
  const [question, setQuestion] = useState('Analyze recent errors and suggest safe repair steps.')
  const [answer, setAnswer] = useState('')
  const [loading, setLoading] = useState(false)

  const refreshStatus = () => plugin.emit('ai:status', (res: Status) => setStatus(res))
  useEffect(refreshStatus, [])

  const ask = () => {
    if (!question.trim() || loading) return
    setLoading(true)
    setAnswer('')
    plugin.emit('ai:ask', (res: AiResponse) => {
      setLoading(false)
      if (!res?.ok) {
        toast(res?.error || lang.actionFailed)
        return
      }
      setAnswer(res.answer || '')
    }, question)
  }

  const ready = !!status?.enabled && !!status?.configured
  return <Box sx={{ minHeight: '100vh', py: 3 }}>
    <Toolbar />
    <Container maxWidth='lg'>
      <Card>
        <CardHeader
          title={lang.ai.title}
          subheader={lang.ai.subtitle}
          action={<Stack direction='row' spacing={1} sx={{ pt: 1 }}>
            <Chip size='small' color={status?.enabled ? 'success' : 'default'} label={status?.enabled ? lang.ai.enabled : lang.ai.disabled} />
            <Chip size='small' color={status?.configured ? 'success' : 'warning'} label={status?.configured ? lang.ai.configured : lang.ai.notConfigured} />
          </Stack>}
        />
        <CardContent>
          <Stack spacing={2}>
            <TextField
              label={lang.ai.question}
              value={question}
              multiline
              minRows={4}
              onChange={e => setQuestion(e.target.value)}
            />
            <Stack direction='row' spacing={1} alignItems='center'>
              <Button variant='contained' disabled={!ready || loading || !question.trim()} onClick={ask}>
                {loading ? lang.ai.analyzing : lang.ai.ask}
              </Button>
              {loading && <CircularProgress size={24} />}
              {status && <Box sx={{ color: 'text.secondary', fontSize: 14 }}>
                {status.model || '-'} · {status.baseUrl || '-'}
              </Box>}
            </Stack>
            {!ready && <Box sx={{ color: 'warning.main' }}>{lang.ai.setupHint}</Box>}
            {answer && <Box sx={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              border: theme => `1px solid ${theme.palette.divider}`,
              borderRadius: 1,
              p: 2,
              bgcolor: 'background.default'
            }}>{answer}</Box>}
          </Stack>
        </CardContent>
      </Card>
    </Container>
  </Box>
}

export default AiAssistant
