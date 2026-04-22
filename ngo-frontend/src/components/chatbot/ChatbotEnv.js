import React, { useState, useRef, useEffect } from 'react';
import { campaignAPI, ngoAPI, urgentNeedsAPI } from '../../services/api';
import './Chatbot.css';

const APP_NAME = process.env.REACT_APP_NAME || 'KindBridge';
const GEMINI_API_KEY = process.env.REACT_APP_GEMINI_API_KEY;
const GEMINI_MODEL = 'gemini-1.5-flash';

const SYSTEM_PROMPT = `You are a helpful assistant for ${APP_NAME}, an NGO donation management platform.
You help guests and users understand:
- How to make donations (monetary, goods, food)
- How campaigns work (guests can view, donors must login to donate)
- How pickup scheduling works for goods/food donations
- What NGOs are available and how to find them
- How to register as a donor or volunteer
- The donation process: register -> browse campaigns -> donate -> payment -> receipt
- Urgent needs alerts shown on the homepage
- Achievements and impact of donations

Be friendly, concise, and guide users toward making donations or registering.
Always encourage users to login/register to make a donation.
Keep answers under 120 words unless the user asks for more detail.`;

const PROJECT_GUIDE = `Project-specific guidance:
- Guests can browse campaigns and NGOs, but users must log in to donate.
- Donors can make monetary donations or goods donations.
- Goods donations can include item details and optional pickup scheduling.
- Donors can view donation history and nearby NGOs from the dashboard.
- Volunteers manage assigned tasks and pickup-related work.
- Admins and NGO admins manage users, campaigns, urgent needs, and reports.
- If asked about current campaigns, urgent needs, or NGO availability, use the provided live context first.
- If a requested detail is not present in the live context, say that clearly instead of inventing it.`;

const buildProjectContext = ({ campaigns = [], ngos = [], urgentNeeds = [] }) => {
  const campaignLines = campaigns.slice(0, 6).map((campaign) => (
    `- ${campaign.title} (${campaign.donationType}, status: ${campaign.campaignStatus || 'active'})`
  ));

  const ngoLines = ngos.slice(0, 6).map((ngo) => (
    `- ${ngo.ngoName} in ${[ngo.city, ngo.state].filter(Boolean).join(', ')}`
  ));

  const urgentNeedLines = urgentNeeds.slice(0, 4).map((need) => (
    `- ${need.title}${need.message ? `: ${need.message}` : ''}`
  ));

  return `Live project context:
Active campaign count: ${campaigns.length}
Partner NGO count: ${ngos.length}
Open urgent need count: ${urgentNeeds.length}

Sample active campaigns:
${campaignLines.length ? campaignLines.join('\n') : '- No active campaigns loaded'}

Sample NGOs:
${ngoLines.length ? ngoLines.join('\n') : '- No NGO data loaded'}

Urgent needs:
${urgentNeedLines.length ? urgentNeedLines.join('\n') : '- No urgent needs loaded'}`;
};

const getFallbackReply = (message, projectContext) => {
  const text = message.toLowerCase();

  if (text.includes('hello') || text.includes('hi') || text.includes('hey')) {
    return 'Hi! I can help with this NGO donation platform, including campaigns, donations, NGOs, pickups, volunteering, and general guidance. Ask me anything.';
  }

  if (text.includes('donat') || text.includes('payment') || text.includes('money')) {
    return 'You can donate by logging in, opening a campaign, and choosing either monetary or goods donation. Monetary donations let you choose an amount and payment method, while goods donations let you add items and optionally schedule pickup.';
  }

  if (text.includes('pickup') || text.includes('goods') || text.includes('item')) {
    return 'For goods donations, choose a campaign, select goods donation, add item details, and optionally add a pickup address, date, and time slot. Volunteers and NGOs can then manage the pickup flow.';
  }

  if (text.includes('register') || text.includes('sign up') || text.includes('create account') || text.includes('login')) {
    return 'You can register as a donor or volunteer from the Register page, then log in from the Login page. Donors can browse campaigns and donate, while volunteers can manage pickup-related tasks.';
  }

  if (text.includes('campaign')) {
    return `You can browse campaigns from the Campaigns page and open any campaign for details and donation options. ${projectContext.includes('Sample active campaigns:') ? 'The chatbot is also using live campaign data from your project.' : ''}`;
  }

  if (text.includes('ngo') || text.includes('nearby') || text.includes('map')) {
    return 'You can explore NGOs from the NGOs page, and donors can search nearby NGOs from the dashboard and view them on the map.';
  }

  if (text.includes('receipt') || text.includes('history') || text.includes('track')) {
    return 'After donating, donors can open My Donations to check donation history, statuses, and receipts when available.';
  }

  if (text.includes('volunteer') || text.includes('task')) {
    return 'Volunteers can register, log in, and manage assigned tasks and pickup work from the volunteer dashboard.';
  }

  if (text.includes('urgent')) {
    return 'Urgent needs are shown on the homepage banner and can be managed by admins or NGO admins.';
  }

  return `I can answer general questions, and I’m also tuned for this NGO donation project. If you ask about campaigns, NGOs, donations, pickups, volunteering, registration, or urgent needs, I can help. ${projectContext ? 'I also have live project context loaded.' : ''}`;
};

const ChatbotEnv = () => {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([
    { role: 'assistant', content: "Hi! I'm KindBot. How can I help you today? Ask me about campaigns, donations, or how to get started!" }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [projectContext, setProjectContext] = useState('Loading live project context...');
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    let mounted = true;

    Promise.allSettled([
      campaignAPI.getActive(),
      ngoAPI.getAll(),
      urgentNeedsAPI.getOpen(),
    ]).then(([campaignsResult, ngosResult, urgentNeedsResult]) => {
      if (!mounted) return;

      const campaigns = campaignsResult.status === 'fulfilled'
        ? campaignsResult.value.data
        : [];
      const ngos = ngosResult.status === 'fulfilled'
        ? ngosResult.value.data
        : [];
      const urgentNeeds = urgentNeedsResult.status === 'fulfilled'
        ? urgentNeedsResult.value.data
        : [];

      setProjectContext(buildProjectContext({ campaigns, ngos, urgentNeeds }));
    }).catch((error) => {
      if (!mounted) return;
      console.error('Failed to load chatbot context:', error);
      setProjectContext('Live project context is currently unavailable. Answer using the known project workflow only.');
    });

    return () => {
      mounted = false;
    };
  }, []);

  const sendMessage = async () => {
    const text = input.trim();
    if (!text || loading) return;

    setInput('');
    const userMsg = { role: 'user', content: text };
    const nextMessages = [...messages, userMsg];
    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);

    try {
      if (!GEMINI_API_KEY) {
        throw new Error('Missing Gemini API key');
      }

      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${GEMINI_API_KEY}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            systemInstruction: {
              parts: [{ text: `${SYSTEM_PROMPT}\n\n${PROJECT_GUIDE}\n\n${projectContext}` }]
            },
            contents: nextMessages.map((message) => ({
              role: message.role === 'assistant' ? 'model' : 'user',
              parts: [{ text: message.content }]
            })),
            generationConfig: {
              temperature: 0.7,
              maxOutputTokens: 300
            }
          })
        }
      );

      if (!response.ok) {
        throw new Error(`Gemini request failed with status ${response.status}`);
      }

      const data = await response.json();
      const reply =
        data.candidates?.[0]?.content?.parts?.map((part) => part.text).join('') ||
        'Sorry, I could not respond right now.';

      setMessages((prev) => [...prev, { role: 'assistant', content: reply }]);
    } catch (error) {
      console.error('Chatbot request failed:', error);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: getFallbackReply(text, projectContext) }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const quickPrompts = [
    'How do I donate?',
    'View active campaigns',
    'How to schedule pickup?',
    'How to register?'
  ];

  return (
    <>
      <button className={`chatbot-fab ${open ? 'open' : ''}`} onClick={() => setOpen(!open)}>
        {open ? (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M18 6L6 18M6 6l12 12" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"/></svg>
        ) : (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" fill="currentColor"/></svg>
        )}
        {!open && <span className="fab-badge">AI</span>}
      </button>

      {open && (
        <div className="chatbot-window">
          <div className="chatbot-header">
            <div className="chatbot-avatar">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" fill="white"/></svg>
            </div>
            <div>
              <h4>KindBot</h4>
              <span>AI Assistant • Always here</span>
            </div>
            <div className="online-dot"></div>
          </div>

          <div className="chatbot-messages">
            {messages.map((msg, i) => (
              <div key={i} className={`chat-msg ${msg.role}`}>
                {msg.role === 'assistant' && (
                  <div className="msg-avatar">K</div>
                )}
                <div className="msg-bubble">{msg.content}</div>
              </div>
            ))}
            {loading && (
              <div className="chat-msg assistant">
                <div className="msg-avatar">K</div>
                <div className="msg-bubble typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          {messages.length === 1 && (
            <div className="quick-prompts">
              {quickPrompts.map((prompt, i) => (
                <button key={i} onClick={() => { setInput(prompt); }}>
                  {prompt}
                </button>
              ))}
            </div>
          )}

          <div className="chatbot-input-area">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKey}
              placeholder="Ask me anything..."
              disabled={loading}
            />
            <button onClick={sendMessage} disabled={!input.trim() || loading} className="send-btn">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
            </button>
          </div>
        </div>
      )}
    </>
  );
};

export default ChatbotEnv;
